/*
*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*
*/
var argscheck = require('cordova/argscheck'),
    channel = require('cordova/channel'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    cordova = require('cordova');

/**
* @constructor
*/
function PocketDuino() {
}

/**
* Get device info
*
* @param {Function} successCallback The function to call when the heading data is available
* @param {Function} errorCallback The function to call when there is an error getting the heading data. (OPTIONAL)
*/
PocketDuino.prototype.setInfo = function(text) {
    var successCallback = function() {
        //utils.alert("PocketDuino callded successful");
    };
    var errorCallback = function(e) {
        utils.alert("[ERROR PocketDuino] Error getText(): " + e);
    }
    exec(successCallback, errorCallback, "PocketDuino", "setInfo", [text]);
};

/**
 *
 * setCallback Test method
 *
 */
PocketDuino.prototype.setCallback = function(successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "PocketDuino", "setCallback", []);
};

/**
 * setCallbackが返ったらイベントを発生してみるテスト
 */
PocketDuino.prototype.callbackListener = function(successCallback, errorCallback) {
    var _cb = function(e) {
        cordova.fireWindowEvent("testcallback", e);
    };
    cordova.exec(_cb, errorCallback, "PocketDuino", "setCallback", []);
};

/**
 * PocketDuino イベントリスナ
 */
PocketDuino.prototype.on = function(event, successCallback, errorCallback) {
    switch (event) {
        case 'attached':
            window.addEventListener('pocketduino.attached', successCallback, false);
            break;
        case 'detached':
            window.addEventListener('pocketduino.detached', successCallback, false);
            break;
        case 'data':
            window.addEventListener('pocketduino.receive', successCallback, false);
            break;
    }
};

/**
 * デバイス アタッチ デタッチ リスナ
 */
PocketDuino.prototype.deviceListener = function() {
    var _cb = function(e) {
        cordova.fireWindowEvent(e.handlerName, e);
    };
    var errorCallback = function(err){
        console.log(err);
    };
    cordova.exec(_cb, errorCallback, "PocketDuino", "listenDevice", []);
};

/**
 * データの受信開始
 */
PocketDuino.prototype.startReceive = function() {
    var _cb = function(e) {
        cordova.fireWindowEvent(e.handlerName, e);
    };
    var errorCallback = function(err){
        console.log(err);
    };
    cordova.exec(_cb, errorCallback, "PocketDuino", "startReceive", []);
};

/**
 * PocketDuino デバイスをオープンする
 */
PocketDuino.prototype.openDevice = function(successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "PocketDuino", "openDevice", []);
};

/**
 * PocketDuino デバイスをクローズする
 */
PocketDuino.prototype.closeDevice = function(successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "PocketDuino", "closeDevice", []);
};

/**
 * PocketDuino デバイスをオープンする
 */
PocketDuino.prototype.loadHexFile = function(fileName, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "PocketDuino", "loadHexFile", [fileName]);
};

/**
 * PocketDuino シリアル書き込み
 */
PocketDuino.prototype.write = function( command, successCallback, errorCallback ){
  cordova.exec( successCallback, errorCallback, "PocketDuino", "write", [command]);

/**
* PocketDuino デバイスオープンから受信までのラッパー
*/
PocketDuino.prototype.run = function(fileName, successCallback, errorCallback) {
    var that = this;
    this.openDevice(
        function() {
            that.loadHexFile(
                fileName,
                function() {
                    //that.setInfo("Start receiving data.");
                    that.startReceive();
                },
                function(err) {
                    that.setInfo("Arduino HEX file load error!");
                    that.setInfo(err);
                }
            );
        },
        function(err) {
            that.closeDevice(
                function() {
                    that.setInfo("PocketDuino Device Closed!");
                },
                function(err) {
                    that.setInfo("PocketDuino Device Close Error!");
                    that.setInfo(err);
                }
            );
            that.setInfo("PocketDuino Device Open Error!");
            that.setInfo(err);
        }
    );
};

/**
* PocketDuino デバイスクローズのラッパー
*/
PocketDuino.prototype.stop = function(fileName, successCallback, errorCallback) {
    var that = this;
    this.closeDevice(
        function() {
            that.setInfo("PocketDuino Device Closed!");
        },
        function(err) {
            that.setInfo("PocketDuino Device Close Error!");
            that.setInfo(err);
        }
    );
};

module.exports = new PocketDuino();
