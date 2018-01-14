# STATION

`station` communicates with [caroid][caroid], our Android module, through a TCP
connection.  We use it to collect various datasets, monitor our RC car,
visualize our data, and ultimately control the car using our deep learning
module `deeprccar`

## Quick Start Guide

Create a `station` environment in which you can run everything except for deep
learning code.

```
conda create --name station --file spec-file.txt
```

If you want to run deep learning code on cpu as well, create a `station-cpu`
environment and see CPU-only TensorFlow Installation below.

```
conda create --name station-cpu --file spec-file.txt
```


If you want to run deep learning code on GPU, create a `station-gpu`
environment and see GPU-enabled TensorFlow Installation below.

```
conda create --name station-gpu --file spec-file.txt
```

## Setup

You need the following software packages to run `station`. Note that because
`station` has multiple features, you don't have to install all packages for a
specific feature. For example, if you just want to collect some dataset, you
don't need [TensorFlow][tf], since it is only used for training the deep
learning module and for autonomous drive.

### Anaconda Installation

Install [anaconda][anaconda] for python3. Here are some basic conda commands to
get you started.

```
export PATH=~/anaconda3/bin/:$PATH # Add it to your path.
conda -V # You can check conda version
conda update conda # You can update your conda
conda search "^python$" # See python versions
conda create -n <env-name> python=<version> [packages] # Create a virtual env
source activate <env-name> # Activate your virtual env
conda info -e # List your virtual envs
conda install -n <env-name> [packages] # Install more packages in <env-name>
source deactivate # Deactivate current virtual env
conda remove -n <env-name> --all # Delete your environment <env-name>
```

If you provide an explicit listing of your environment, you can create an
idential environment later on. Here is how you do it.

```
# Create a spec-file and keep it in your git repo
conda list --explicit > spec-file.txt
```

From a spec-file one can create an idential environment.

```
# Create an idential environment from the spec-file
conda create --name <env-name> --file spec-file.txt
```

### OpenCV Installation
If you don't want to create your environment from an existing spec-file, here
is how you can install `opencv3` on your environment.

```
source activate <env-name>
conda install -c https://conda.binstar.org/menpo opencv3
```

See [this tutorial][cv2_install] for more information.

### TensorFlow Installation

The spec-file won't include TensorFlow installation. It is possible to install
two different versions of TensorFlow, CPU-only and GPU-enabled if you have a
GPU.  You can create a conda environment for each TensorFlow version to install
both versions on the same machine.

#### CPU-only TensorFlow Installation

Following instructions help you install CPU-only TensorFlow.

```
source activate <env-name-cpu>
# URL for TensorFlow installation assuming python3.6 for <env-name-cpu>
TF_PYTHON_URL=https://storage.googleapis.com/tensorflow/linux/cpu/tensorflow-1.1.0rc2-cp36-cp36m-linux_x86_64.whl
pip install --ignore-installed --upgrade $TF_PYTHON_URL
```

#### GPU-enabled TensorFlow Installation

Following instructions help you install GPU-enabled TensorFlow.

Install kernel headers.

```
sudo apt-get install linux-headers-$(uname -r)
```

Download and install [CUDA Toolkit 8.0][cuda]. Pick *Installer Type*: `deb
(network)` and follow its *Installation Instructions*.

Fore more information see [NVIDIA CUDA Installation Guide for
Linux][cuda_install].


Download [cuDNN v5.1][cuDNN]. You first need to register before the download.
Extract it to `/usr/local` directory, in which CUDA Toolkit 8.0 was installed.

```
sudo tar -xzf cudnn-8.0-linux-x64-v5.1.tgz -C /usr/local
```

Add the following lines to your `~/.bashrc` file.

```
export PATH=/usr/local/cuda/bin${PATH:+:${PATH}}
export LD_LIBRARY_PATH=/usr/local/cuda/lib64${LD_LIBRARY_PATH:+:${LD_LIBRARY_PATH}}
```

and source it with `source ~/.bashrc`.

Install NVIDIA CUDA Profile Tools Interface.

```
sudo apt-get install libcupti-dev
```

Now we are ready to install GPU-enabled TensorFlow.

```
source activate <env-name-gpu>
# URL for TensorFlow installation assuming python3.6 for <env-name-gpu>
TF_PYTHON_URL=https://storage.googleapis.com/tensorflow/linux/gpu/tensorflow_gpu-1.1.0rc2-cp36-cp36m-linux_x86_64.whl
pip install --ignore-installed --upgrade $TF_PYTHON_URL
```

Make sure the python version of your environment is consistent with
`TF_PYTHON_URL`.  See [Installing TensorFlow on Ubuntu][tf_install] for
different `TF_PYTHON_URL` values.


[caroid]: ../caroid
[tf]: https://www.tensorflow.org
[anaconda]: https://www.continuum.io/downloads
[cv2_install]: https://rivercitylabs.org/up-and-running-with-opencv3-and-python-3-anaconda-edition/
[tf_install]: https://www.tensorflow.org/install/install_linux
[cuda]: https://developer.nvidia.com/cuda-downloads
[cuda_install]: http://docs.nvidia.com/cuda/cuda-installation-guide-linux
[cuDNN]: https://developer.nvidia.com/cudnn
