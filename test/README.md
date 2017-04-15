# TEST

This is a temporary directory for testing our Android and Arduino codes which
can be found in caroid and carino directories, respectively. This directory
should be ultimately replaced with our station which controls and monitors the
RC car over a TCP connection. For the time being we use this directory to
collect our training data.

## Setup

Install [anaconda][1] for python3. Here are some basic conda commands to get
you started.

```
$ export PATH=~/anaconda3/bin/:$PATH # Add it to your path.
$ conda -V # You can check conda version
$ conda update conda # You can update your conda
$ conda search "^python$" # See python versions
$ conda create -n <env-name> python=<version> [packages] # Create a virtual env
$ source activate <env-name> # Activate your virtual env
$ conda info -e # List your virtual envs
$ conda install -n <env-name> [packages] # Install more packages in <env-name>
$ source deactivate # Deactivate current virtual env
$ conda remove -n <env-name> --all # Delete your environment <env-name>
```

If you provide an explicit listing of your environment, you can create an
idential environment later on. Here is how you do it.

```
# Create a spec-file and keep it in your git repo
conda list --explicit > spec-file.txt
```

From a spec-file one can create an idential environment.

```
# Create the idential environment from spec-file.txt
conda create --name <env-name> --file spec-file.txt
```

If you don't want to create your environment from the spec-file.txt, you might
find [this tutorial][2] helpful to get up and running with anaconda and opencv.

[1]: https://www.continuum.io/downloads
[2]: https://rivercitylabs.org/up-and-running-with-opencv3-and-python-3-anaconda-edition/
