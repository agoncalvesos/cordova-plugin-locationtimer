var exec = require('cordova/exec');

exports.init = function (success, error, uuid, postUrl, timer) {
    exec(success, error, 'LocationTimer', 'initialize', [uuid, postUrl, timer]);
};
