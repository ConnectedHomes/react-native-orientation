const Orientation = require('react-native').NativeModules.Orientation;
const DeviceEventEmitter = require('react-native').DeviceEventEmitter;

const listeners = {};
const orientationDidChangeEvent = 'orientationDidChange';
const specificOrientationDidChangeEvent = 'specificOrientationDidChange';
const orientationLockSettingsDidChanged = 'orientationLockSettingsDidChanged';

let id = 0;
const META = '__listener_id';

function getKey(listener) {
  if (!listener.hasOwnProperty(META)) {
    if (!Object.isExtensible(listener)) {
      return 'F';
    }

    Object.defineProperty(listener, META, {
      value: `L${++id}`,
    });
  }

  return listener[META];
}

module.exports = {
  start() {
    return Orientation.start();
  },

  isOrientationLockedInSettings(cb) {
    Orientation.isOrientationLockedInSettings((error, result) => {
      cb(error, result);
    });
  },

  isOrientationLockedInSettings(cb) {
    Orientation.isOrientationLockedInSettings((error, result) => {
      cb(error, result);
    });
  },

  getOrientation(cb) {
    Orientation.getOrientation((error, orientation) => {
      cb(error, orientation);
    });
  },

  lockToPortrait() {
    Orientation.lockToPortrait();
  },

  lockToLandscape() {
    Orientation.lockToLandscape();
  },

  lockToLandscapeRight() {
    Orientation.lockToLandscapeRight();
  },

  lockToLandscapeLeft() {
    Orientation.lockToLandscapeLeft();
  },

  unlockAllOrientations() {
    Orientation.unlockAllOrientations();
  },

  addOrientationListener(cb) {
    const key = getKey(cb);
    listeners[key] = DeviceEventEmitter.addListener(
      orientationDidChangeEvent,
      (body) => {
        cb(body);
      },
    );
  },

  removeOrientationListener(cb) {
    const key = getKey(cb);

    if (!listeners[key]) {
      return;
    }

    listeners[key].remove();
    listeners[key] = null;
  },

  addOrientationLockSettingsDidChanged(cb) {
    const key = getKey(cb);
    listeners[key] = DeviceEventEmitter.addListener(
      orientationLockSettingsDidChanged,
      (body) => {
        cb(body.orientation);
      },
    );
  },

  removeOrientationLockSettingsDidChanged(cb) {
    const key = getKey(cb);

    if (!listeners[key]) {
      return;
    }

    listeners[key].remove();
    listeners[key] = null;
  },

  getInitialOrientation() {
    return Orientation.initialOrientation;
  },
};
