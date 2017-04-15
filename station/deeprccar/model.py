#!/usr/bin/env python
# -*- coding: utf-8 -*-

import functools

import numpy as np

import tensorflow as tf
from tensorflow.contrib import slim
from tensorflow.python.util import nest

import dataset as ds


def doublewrap(function):
    """
    A decorator decorator, allowing to use the decorator to be used without
    parentheses if not arguments are provided. All arguments must be optional.
    """
    @functools.wraps(function)
    def decorator(*args, **kwargs):
        if len(args) == 1 and len(kwargs) == 0 and callable(args[0]):
            return function(args[0])
        else:
            return lambda wrapee: function(wrapee, *args, **kwargs)
    return decorator


@doublewrap
def define_scope(function, scope=None, *args, **kwargs):
    """
    A decorator for functions that define TensorFlow operations. The wrapped
    function will only be executed once. Subsequent calls to it will directly
    return the result so that operations are added to the graph only once.
    The operations added by the function live within a tf.variable_scope(). If
    this decorator is used with arguments, they will be forwarded to the
    variable scope. The scope name defaults to the name of the wrapped
    function.
    """
    attribute = '_cache_' + function.__name__
    name = scope or function.__name__

    @property
    @functools.wraps(function)
    def decorator(self):
        if not hasattr(self, attribute):
            with tf.variable_scope(name, *args, **kwargs):
                setattr(self, attribute, function(self))
        return getattr(self, attribute)
    return decorator


class Model:
    def __init__(self, mean, stddev, training_args):
        self._mean = mean
        self._stddev = stddev
        self._args = training_args

        self._internal_cell = None
        self._initial_state = None
        self._gt_prev_state = None
        self._au_prev_state = None

        tf.add_to_collection("lookback_length",
                             tf.constant(self._args.lookback_length,
                                         dtype=tf.int32,
                                         name="lookback_length"))

        tf.add_to_collection("stats",
                             tf.constant(self._mean,
                                         dtype=tf.float32,
                                         name="mean"))
        tf.add_to_collection("stats",
                             tf.constant(self._stddev,
                                         dtype=tf.float32,
                                         name="stddev"))

        self.placeholders
        self.visual_features
        self.normalized_commands
        self.ground_truth_inference
        self.autoregressive_inference
        self.predictions
        self.loss
        self.optimization

    @define_scope
    def placeholders(self):
        self._imfiles = tf.placeholder(dtype=tf.string,
                                      shape=[None, None],
                                      name="image_files")
        self._commands = tf.placeholder(dtype=tf.float32,
                                        shape=[None, None, ds.NUM_COMMANDS],
                                        name="commands")
        self._sqlen = tf.placeholder_with_default(1,
                                                  shape=[],
                                                  name="sequence_length")
        self._bsize = tf.placeholder_with_default(1,
                                                  shape=[],
                                                  name="batch_size")
        self._keep_prob = tf.placeholder_with_default(1.0,
                                                      shape=[],
                                                      name="keep_prob")

        tf.add_to_collection("placeholders", self._imfiles)
        tf.add_to_collection("placeholders", self._commands)
        tf.add_to_collection("placeholders", self._sqlen)
        tf.add_to_collection("placeholders", self._bsize)
        tf.add_to_collection("placeholders", self._keep_prob)

        return (self._imfiles, self._commands, self._sqlen,
                self._bsize, self._keep_prob)

    @define_scope
    def input_images(self):
        num_images = (self._sqlen + self._args.lookback_length) * self._bsize
        images = tf.map_fn(lambda x: tf.image.decode_jpeg(tf.read_file(x)),
                           tf.reshape(self._imfiles, shape=[num_images]),
                           dtype=tf.uint8)
        images.set_shape([None, ds.HEIGHT, ds.WIDTH, ds.CHANNELS])
        return images

    @define_scope
    def visual_features(self):
        input_images = self.input_images
        tf.add_to_collection("input_images", input_images)

        images = -1.0 + 2.0 * tf.cast(input_images, tf.float32) / 255.0

        bsize =  self._bsize                # Batch size
        lblen = self._args.lookback_length  # Lookback length
        sqlen = self._sqlen                 # Sequence length

        images = tf.reshape(images, shape=[bsize, lblen + sqlen,
                                           ds.HEIGHT, ds.WIDTH, ds.CHANNELS])

        net = slim.convolution(images,
                               num_outputs=64,
                               kernel_size=[3, 12, 12],
                               stride=[1, 6, 6],
                               padding="VALID")
        net = tf.nn.dropout(x=net, keep_prob=self._keep_prob)
        # Height x Width x Channel
        hwc = np.prod(net.get_shape().as_list()[2:])
        aux1 = slim.fully_connected(tf.reshape(net[:, -sqlen:, :, :, :],
                                               [bsize, sqlen, hwc]),
                                    128, activation_fn=None)

        net = slim.convolution(net,
                               num_outputs=64,
                               kernel_size=[2, 5, 5],
                               stride=[1, 2, 2],
                               padding="VALID")
        net = tf.nn.dropout(x=net, keep_prob=self._keep_prob)
        # Height x Width x Channel
        hwc = np.prod(net.get_shape().as_list()[2:])
        aux2 = slim.fully_connected(tf.reshape(net[:, -sqlen:, :, :, :],
                                               [bsize, sqlen, hwc]),
                                    128, activation_fn=None)

        net = slim.convolution(net,
                               num_outputs=64,
                               kernel_size=[2, 5, 5],
                               stride=[1, 1, 1],
                               padding="VALID")
        net = tf.nn.dropout(x=net, keep_prob=self._keep_prob)
        # Height x Width x Channel
        hwc = np.prod(net.get_shape().as_list()[2:])
        aux3 = slim.fully_connected(tf.reshape(net[:, -sqlen:, :, :, :],
                                               [bsize, sqlen, hwc]),
                                    128, activation_fn=None)

        net = slim.convolution(net,
                               num_outputs=64,
                               kernel_size=[2, 5, 5],
                               stride=[1, 1, 1],
                               padding="VALID")
        net = tf.nn.dropout(x=net, keep_prob=self._keep_prob)
        # At this point the tensor 'net' is of shape
        # batch_size x seq_len x Height x Width x Channel
        # Height x Width x Channel
        hwc = np.prod(net.get_shape().as_list()[2:])
        aux4 = slim.fully_connected(tf.reshape(net,
                                               [bsize, sqlen, hwc]),
                                    128, activation_fn=None)

        net = slim.fully_connected(tf.reshape(net,
                                              [bsize, sqlen, hwc]),
                                   1024, activation_fn=tf.nn.relu)
        net = tf.nn.dropout(x=net, keep_prob=self._keep_prob)
        net = slim.fully_connected(net, 512, activation_fn=tf.nn.relu)
        net = tf.nn.dropout(x=net, keep_prob=self._keep_prob)
        net = slim.fully_connected(net, 256, activation_fn=tf.nn.relu)
        net = tf.nn.dropout(x=net, keep_prob=self._keep_prob)
        net = slim.fully_connected(net, 128, activation_fn=None)

        # aux[1-4] are residual connections (shortcuts)
        visual_features = _layer_norm(tf.nn.elu(
            net + aux1 + aux2 + aux3 + aux4))

        num_outputs = visual_features.get_shape().as_list()[-1]
        visual_features = tf.reshape(visual_features,
                                     [bsize, sqlen, num_outputs])

        visual_features = tf.nn.dropout(x=visual_features,
                                        keep_prob=self._keep_prob)
        return visual_features

    @define_scope
    def normalized_commands(self):
        return(self._commands - self._mean) / self._stddev

    @define_scope(scope="inference")
    def ground_truth_inference(self):

        if self._internal_cell is None:
            self._internal_cell = tf.contrib.rnn.LSTMCell(
                num_units=self._args.num_units,
                num_proj=self._args.num_proj)

        ground_truth_cell = _SamplingRNNCell(
            num_outputs=ds.NUM_COMMANDS, use_ground_truth=True,
            internal_cell=self._internal_cell)

        if self._initial_state is None:
            self._initial_state = _get_initial_state(
                ground_truth_cell.state_size, self._bsize)
            self.ground_truth_prev_state = self._initial_state
            self.autoregressive_prev_state = self._initial_state

        gt_normalized_predictions, gt_next_state = (
            self._ground_truth_predictor(ground_truth_cell,
                                         self.ground_truth_prev_state,
                                         self.visual_features,
                                         self.normalized_commands))

        return gt_normalized_predictions, gt_next_state

    @define_scope(scope="inference")
    def autoregressive_inference(self):
        autoregressive_cell = _SamplingRNNCell(
            num_outputs=ds.NUM_COMMANDS, use_ground_truth=False,
            internal_cell=self._internal_cell)

        flat_state = nest.flatten(self.autoregressive_prev_state)
        for state in flat_state:
            tf.add_to_collection("prev_state", state)

        au_normalized_predictions, au_next_state = (
            self._autoregressive_predictor(autoregressive_cell,
                                           self.autoregressive_prev_state,
                                           self.visual_features,
                                           self.normalized_commands))

        flat_state = nest.flatten(au_next_state)
        for state in flat_state:
            tf.add_to_collection("next_state", state)

        return au_normalized_predictions, au_next_state

    @define_scope
    def predictions(self):
        au_normalized_predictions, _ = self.autoregressive_inference
        au_predictions = tf.add(au_normalized_predictions * self._stddev,
                                self._mean, name="autoregressive_predictions")

        tf.add_to_collection("predictions", au_predictions)
        return au_predictions

    @define_scope
    def loss(self):
        gt_normalized_predictions, _ = self.ground_truth_inference
        au_normalized_predictions, _ = self.autoregressive_inference
        normalized_commands = self.normalized_commands

        gt_mse = tf.reduce_mean(tf.squared_difference(
            gt_normalized_predictions, normalized_commands), name="gt_mse")

        au_mse = tf.reduce_mean(tf.squared_difference(
            au_normalized_predictions, normalized_commands), name="au_mse")

        au_steering_mse = tf.reduce_mean(tf.squared_difference(
            au_normalized_predictions[:, :, 0], normalized_commands[:, :, 0]),
                                         name="au_steering_mse")

        gt_w = self._args.ground_truth_weight
        au_w = self._args.autoregressive_weight
        loss = tf.add(au_steering_mse,
                      (gt_w * gt_mse + au_w * au_mse),
                      name="loss")
        return loss

    @define_scope
    def optimization(self):
        learning_rate = self._args.learning_rate
        clip_norm = self._args.clip_norm
        optimizer = tf.train.AdamOptimizer(learning_rate=learning_rate)
        gradvars = optimizer.compute_gradients(self.loss)
        gradients, variables = zip(*gradvars)
        gradients, _ = tf.clip_by_global_norm(gradients, clip_norm)
        return optimizer.apply_gradients(zip(gradients, variables))

    @property
    def ground_truth_prev_state(self):
        return self._gt_prev_state

    @ground_truth_prev_state.setter
    def ground_truth_prev_state(self, next_state):
        self._gt_prev_state = next_state

    @property
    def ground_truth_next_state(self):
        _, next_state = self.ground_truth_inference
        return next_state

    @property
    def autoregressive_prev_state(self):
        return self._au_prev_state

    @autoregressive_prev_state.setter
    def autoregressive_prev_state(self, next_state):
        self._au_prev_state = next_state

    @property
    def autoregressive_next_state(self):
        _, next_state = self.autoregressive_inference
        return next_state

    def _ground_truth_predictor(self,
                                cell,
                                prev_state,
                                visual_features,
                                normalized_commands):

        inputs = (visual_features, normalized_commands)
        prev_state = _deep_copy_state(prev_state,
                                      "gt_prev_state")
        with tf.variable_scope("predictor"):
            normalized_prediction, next_state = (
                tf.nn.dynamic_rnn(
                    cell=cell,
                    inputs=inputs,
                    initial_state=prev_state,
                    dtype=tf.float32, swap_memory=True, time_major=False))

        return normalized_prediction, next_state

    def _autoregressive_predictor(self,
                                  cell,
                                  prev_state,
                                  visual_features,
                                  normalized_commands):

        zero_commands = tf.zeros_like(normalized_commands,
                                      dtype=tf.float32)
        inputs = (visual_features, zero_commands)
        prev_state = _deep_copy_state(prev_state,
                                      "au_prev_state")
        with tf.variable_scope("predictor", reuse=True):
            normalized_prediction, next_state = (
                tf.nn.dynamic_rnn(
                    cell=cell,
                    inputs=inputs,
                    initial_state=prev_state,
                    dtype=tf.float32, swap_memory=True, time_major=False))

        return normalized_prediction, next_state


def _get_initial_state(complex_state_tuple_sizes, batch_size):
    flat_sizes = nest.flatten(complex_state_tuple_sizes)
    initial_state_flat = [
        tf.tile(
            multiples=[batch_size, 1],
            input=tf.get_variable(
                "initial_state_{}".format(i),
                initializer=tf.zeros_initializer(), shape=([1, s]),
                dtype=tf.float32))
        for i, s in enumerate(flat_sizes)]
    initial_state = nest.pack_sequence_as(complex_state_tuple_sizes,
                                          initial_state_flat)
    return initial_state


def _deep_copy_state(complex_state_tuple, name):
    flat_state = nest.flatten(complex_state_tuple)
    flat_state_copy = [tf.identity(s, name="{}_{}".format(name, i))
                       for i, s in enumerate(flat_state)]
    state_copy = nest.pack_sequence_as(complex_state_tuple,
                                       flat_state_copy)
    return state_copy


def _layer_norm(inputs):
    return tf.contrib.layers.layer_norm(
        inputs=inputs,
        center=True, scale=True,
        activation_fn=None, trainable=True)


class _SamplingRNNCell(tf.contrib.rnn.RNNCell):
    def __init__(self, num_outputs, use_ground_truth, internal_cell):
        self._num_outputs = num_outputs
        self._use_ground_truth = use_ground_truth  # Boolean
        self._internal_cell = internal_cell  # May be LSTM or GRU or anything

    @property
    def state_size(self):
        # Previous output and bottleneck state
        return self._num_outputs, self._internal_cell.state_size

    @property
    def output_size(self):
        return self._num_outputs  # steering_cmd, speed_cmd

    def __call__(self, inputs, state, scope=None):
        (visual_features, current_ground_truth) = inputs
        prev_output, prev_state_internal = state
        context = tf.concat([prev_output, visual_features], 1)

        # Here the internal cell (e.g. LSTM) is called
        new_output_internal, new_state_internal = (
            self._internal_cell(context, prev_state_internal))
        new_output = tf.contrib.layers.fully_connected(
            inputs=tf.concat(
                [new_output_internal, prev_output, visual_features], 1),
            num_outputs=self._num_outputs,
            activation_fn=None,
            scope="OutputProjection")
        # If self._use_ground_truth is True,
        # we pass the ground truth as the state;
        # otherwise, we use the model's predictions
        return new_output, (current_ground_truth if self._use_ground_truth
                            else new_output, new_state_internal)


if __name__ == "__main__":
    from training_args import args
    (training_set, validation_set), (training_mean, training_stddev) = (
        ds.parse_training_dataset(args.dataset_dir, args.sequence_length,
                                  args.batch_size))
    print("Training Mean: {}, Training Standard Deviation: {}"
          .format(training_mean, training_stddev))
    print("Training set length: {}".format(len(training_set)))
    print("Validation set length: {}".format(len(validation_set)))
    training_iter = ds.BatchIter(training_set, args.batch_size,
                                 args.lookback_length, args.sequence_length)
    print("The number of training batches: {}"
          .format(len(training_iter)))

    model = Model(training_mean, training_stddev, args)
    with tf.Session() as sess:
        init = tf.global_variables_initializer()
        sess.run(init)
        for index, batch in enumerate(training_iter):
            feed_dict = dict(zip(model.placeholders,
                                 [batch[0], batch[1], args.sequence_length,
                                  args.batch_size, args.keep_prob]))
            loss = sess.run([model.loss,
                             model.optimization], feed_dict=feed_dict)
            print(index, loss)
