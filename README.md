# AFMLuminescence

## Goal

This software aim to reproduce the luminscence of a quantum dot (QD) layer. It does so by simulating a QD layer with electrons, and simulate their recombinations. The obtained spectra can be compared to one taken from an experiment, and the QD density can then be adjusted in order to extract the QD distribution from the luminescence.

## Animation

The program create a visualisation of what is happening in the form of an animation. For performance purposes, the animation run at 2 frame/second and the simulation run with a timestep of 1 fs/cycle. In the visualisation, the electrons are shown as black dots and the QDs as green circles. When a recombination occurs in a QD during the last cycle, it change its color to red.

## Dependency

* JDK 11
* Java FX 11
* [audreyazura/CommonUtils](https://github.com/audreyazura/CommonUtils)
* [BigDecimalMath (by Dr. Richard J. Mathard)](https://arxiv.org/abs/0908.3030v4)
* [PCG (java implementation by Kilian Brachtendorf)](https://github.com/KilianB/pcg-java)
