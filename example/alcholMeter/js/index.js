/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
var app = {
    // Application Constructor
    initialize: function() {
        this.bindEvents();
    },
    // Bind Event Listeners
    //
    // Bind any events that are required on startup. Common events are:
    // 'load', 'deviceready', 'offline', and 'online'.
    bindEvents: function() {
        document.addEventListener('deviceready', this.onDeviceReady, false);
    },
    // deviceready Event Handler
    //
    // The scope of 'this' is the event. In order to call the 'receivedEvent'
    // function, we must explicitly call 'app.receivedEvent(...);'
    onDeviceReady: function() {
        app.receivedEvent('deviceready');
    },
    // Update DOM on a Received Event
    receivedEvent: function(id) {
        var parentElement = document.getElementById(id);
        var listeningElement = parentElement.querySelector('.listening');
        var receivedElement = parentElement.querySelector('.received');

        listeningElement.setAttribute('style', 'display:none;');
        receivedElement.setAttribute('style', 'display:block;');

        console.log('Received Event: ' + id);

        /**
         * アルコール ％のざっくり計算
         */
        var getPercentage = function(val) {
            var min = 450; //だいたいの下限値
            var max = 850; //だいたいの上限値
            var ret;
            if (isNaN(val)) return 0;
            if (val < min) {
                ret = 0;
            } else {
                ret = Math.floor( (val / max) * 100 );
                if(ret > 100) ret = 100;
            }
            return ret;
        };

        // HexStringを文字列に変換するメソッド群
        // TODO: 将来的にはライブラリオプションとして {percer: function(data){} } 辺りを設定すると
        //       パーサ実行後のデータが振ってくるように修正したい。
        var hexToByte = function ( s ){
          return parseInt( s, 16);
        };
        var hexStringToByteArray =  function(hexString){
          if( !hexString ){ return [] }
          var result = [];

          for (var i = 0; i < hexString.length; i+=2) {
            result.push(hexToByte(hexString.substr(i,2)));
          }
          return result;
        };
        var decodeHexString = function( hexString ){
          return hexStringToByteArray( hexString )
            .map( function( code){ return String.fromCharCode( code ) })
            .join("");
        };

        // 読みとった文字列データより、アルコール度数データをピックアップする
        var pickUpAlcoholData = function( data ){
          // TODO : s to \r プロトコル判定メソッドのparcer化(前述)
          var decodeS0DData = function( str ){
            return str.substring( str.indexOf("s") + 1 , str.indexOf("\r"));
          };
          // TODO :NaNのハンドリング
          return parseInt( decodeS0DData( data ) );
        };

        // PocketDuino データ受信リスナ
        var elAlcoholMeter = document.getElementById('alcoholMater');
        pocketduino.on('data', function(e) {
            var decodedData = decodeHexString( e.data );
            console.log(e.data);
            elAlcoholMeter.innerHTML = getPercentage( pickUpAlcoholData( decodedData ) );
        });
        // PocketDuino アタッチリスナ
        pocketduino.on('attached', function() {
            pocketduino.run('AlcoholSensor.hex');
        });
        // PocketDuino デタッチリスナ
        pocketduino.on('detached', function() {
            elAlcoholMeter.innerHTML = "0";
            pocketduino.stop();
        });
        pocketduino.deviceListener();
        pocketduino.run('AlcoholSensor.hex');

    },
    onPause: function() {
        document.getElementById('alcoholMater').innerHTML = "0";
        pocketduino.setInfo("!!! Paused !!!");
        pocketduino.stop();
    },
    onResume: function() {
        pocketduino.setInfo("!!! Resumed !!!");
        pocketduino.run('AlcoholSensor.hex');
    }
};

app.initialize();
