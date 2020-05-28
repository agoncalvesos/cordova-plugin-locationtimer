var exec = require('cordova/exec');

exports.init = function (success, error, uuid, timer, postUrl) {
    exec(success, error, 'LocationTimer', 'init', [uuid, timer, postUrl]);
};
