#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import csv

import numpy as np

import tensorflow as tf

import dataset as ds
from model import Model
from training_args import args
import utils


def do_epoch(sess, model, iterator, mode, epoch):
    total_steps = len(iterator)
    total_loss = np.float128(0.0)
    gt_next_state, au_next_state = None, None
    prefix_str = "{epoch:03d}/{epochs} {mode: <10} |{avrg_loss:.4f}| "

    records = {}
    for step, batch in enumerate(iterator):
        imfiles, commands = batch
        feed_list = [imfiles, commands, args.sequence_length,
                     args.batch_size, args.keep_prob]
        feed_dict = dict(zip(model.placeholders, feed_list))

        if gt_next_state is not None:
            feed_dict.update({model.ground_truth_prev_state: gt_next_state})
        if au_next_state is not None:
            feed_dict.update({model.autoregressive_prev_state: au_next_state})

        if mode == "Training":
            au_next_state, gt_next_state, loss, _ = sess.run(
                [model.autoregressive_next_state,
                 model.ground_truth_next_state,
                 model.loss,
                 model.optimization], feed_dict=feed_dict)
        elif mode == "Validation":
            # Keep probability is 1.0 for validation
            feed_dict.update({model.placeholders[-1]: 1.0})

            au_next_state, loss, predictions = sess.run(
                [model.autoregressive_next_state,
                 model.loss,
                 model.predictions], feed_dict=feed_dict)
            imfiles = imfiles[:, args.lookback_length:].flatten()
            shape = commands.shape
            commands = np.reshape(commands, (shape[0]*shape[1], -1))
            predictions = np.reshape(predictions, commands.shape)
            record = np.stack([commands, predictions,
                               np.square(commands - predictions)])
            for index, imfile in enumerate(imfiles):
                records[imfile] = record[:, index]

        total_loss += loss
        prefix = prefix_str.format(epoch=epoch,
                                   epochs=args.num_epochs,
                                   mode=mode,
                                   avrg_loss=total_loss / (step + 1))
        utils.progressbar(total_steps, step + 1, prefix)
    avrg_loss = total_loss / total_steps
    return (avrg_loss, records) if mode == "Validation" else avrg_loss


def do_validation(sess, model, validation_iter, epoch, saver):
    tot = np.float128(0.0)
    loss, records = do_epoch(sess, model, validation_iter, "Validation", epoch)
    if do_validation.min_loss is None or do_validation.min_loss > loss:
        do_validation.min_loss = loss
        ckpt = "{}/deeprccar-model".format(args.checkpoint_dir)
        saver.save(sess, ckpt)
        validf = "{}/validation-epoch{:03d}.txt".format(args.checkpoint_dir,
                                                        epoch)
        with open(validf, "w") as valid_f:
            for imfile, record in records.items():
                print(imfile, record[0], record[1], record[2], file=valid_f)
                tot += record[-1]
        print("Unormalized Validation RMSE: ", np.sqrt(tot/len(records)))
        print("Model saved at ", ckpt)
    return loss
do_validation.min_loss = None

def get_saver():
    saver = tf.train.Saver()
    return saver


def do_training():
    if not os.path.isdir(args.checkpoint_dir):
        os.makedirs(args.checkpoint_dir)

    (training_set, validation_set), (training_mean, training_stddev) = (
        ds.parse_training_dataset(args.dataset_dir, args.sequence_length,
                                  args.batch_size, args.validation_percentage))

    print("Training Set Mean: {}".format(training_mean))
    print("Training Set Standard Deviation: {}".format(training_stddev))
    print("Training Set Length: {}".format(len(training_set)))
    print("Validation Set Length: {}".format(len(validation_set)))

    training_iter = ds.BatchIter(training_set, args.batch_size,
                                 args.lookback_length, args.sequence_length)
    validation_iter = ds.BatchIter(validation_set, args.batch_size,
                                   args.lookback_length, args.sequence_length)

    csv_file = open("{}/{}.csv".format(args.checkpoint_dir, "training"), "w")
    csv_writer = csv.writer(csv_file, delimiter=';')
    csv_writer.writerow(("epoch", "validation", "training"))
    csv_file.flush()

    model = Model(training_mean, training_stddev, args)
    saver = tf.train.Saver()
    init = tf.global_variables_initializer()
    ckpt = tf.train.latest_checkpoint(args.checkpoint_dir)
    with tf.Session() as sess:
        if ckpt:
            print("Restoring model from ", ckpt)
            saver.restore(sess, save_path=ckpt)
        else:
            sess.run(init)
        for epoch in range(1, args.num_epochs + 1):
            vloss = do_validation(sess, model, validation_iter, epoch, saver)
            tloss = do_epoch(sess, model, training_iter, "Training", epoch)
            csv_writer.writerow((epoch, vloss, tloss))
            csv_file.flush()
        csv_file.close()

do_training()
