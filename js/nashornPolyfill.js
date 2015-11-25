'use strict';


//httpClient as org.apache.http.impl.nio.client.CloseableHttpAsyncClient
//timer as java.util.Timer
//phaser as java.util.concurrent.Phaser
//logger as StringBuilder


//helper for async callback results
function makeResult(obj) {
    resultJSON = JSON.stringify(obj, null, 2);
}


//console
var console = {
    log: function (content) {
        print(content);
        logger.append(content + '\r\n');
    },
    error: function (content) {
        content = 'error: '+content;
        print(content);
        logger.append(content + '\r\n');
    }
};
//Promises
load('https://raw.githubusercontent.com/taylorhakes/promise-polyfill/master/Promise.min.js');



var onTaskFinished = function () {
    phaser.arriveAndDeregister();
};

function setTimeout(fn, milliseconds) {
    var phase = phaser.register();
    var canceled = false;
    var args = [].slice.call(arguments, 2, arguments.length);
    timer.schedule(function () {
        if (canceled) {
            return;
        }

        try {
            fn.apply(this, args);
        } catch (e) {
            print(e);
        } finally {
            onTaskFinished();
        }
    }, milliseconds);

    return function () {
        onTaskFinished();
        canceled = true;
    };
}
function clearTimeout (cancel) {
    cancel();
};

function XMLHttpRequest() {
    

    this.onreadystatechange = function () { };
    this.readyState = 0;
    this.response = null;
    this.responseText = null;
    this.responseType = '';
    this.responseXML = null;
    this.status = null;
    this.statusText = null;
    this.timeout = 0;
    this.ontimeout = function () { };
    this.upload = null;
    this.withCredentials = false;

    this.UNSENT = 0;
    this.OPENED = 1;
    this.HEADERS_RECEIVED = 2;
    this.LOADING = 3;
    this.DONE = 4;

    
    this._method = 'GET';
    this._url = 'http://localhost/';
    this._headers = {};
    this._async = true;
    this._overrideMimeType = null;
    this._responseHeaders = {};
}

XMLHttpRequest.prototype.abort = function () {

    //no idea how to do it or if even needed
}

XMLHttpRequest.prototype.getAllResponseHeaders = function () {
    var response = '';
    for (var header in this._responseHeaders) {
        if (this._responseHeaders.hasOwnProperty(header)) {
            response += header + ': ' + this._responseHeaders[header] + '\r\n';
        }
    }
    if (response.length == 0) {
        return null;
    }
    return response;
}

XMLHttpRequest.prototype.getResponseHeader = function (header) {
    if (this._responseHeaders.hasOwnProperty(header)) {
        return this._responseHeaders[header];
    }
    return null;
}

XMLHttpRequest.prototype.open = function (method, url, async, user, password) {
    this._method = method.trim().toUpperCase();
    this._url = url;
    this._async = async || true;
    this.readyState = 1;
    setTimeout(this.onreadystatechange, 0);
}

XMLHttpRequest.prototype.overrideMimeType = function (mimetype) {
    this._overrideMimeType = mimetype;
}

XMLHttpRequest.prototype.send = function (data) {
    
    var self = this;
    var methodPascalCase = this._method[0].toUpperCase() + this._method.substring(1).toLocaleLowerCase();
    
    //get correct request class
    var Request = Java.type('org.apache.http.client.methods.Http' + methodPascalCase);
    
    //set url
    var request = new Request(this._url);
    //add headers
    for (var header in this._headers) {
        if (this._headers.hasOwnProperty(header)) {
            request.addHeader(header, this._headers[header]);
        }
    }
    //set body
    if (methodPascalCase == 'Post' || methodPascalCase == 'Put') {
        request.setEntity(new org.apache.http.entity.StringEntity(data, 'UTF-8'));
    }

    phaser.register();
    httpclient.execute(request, new org.apache.http.concurrent.FutureCallback({
        completed: function (response) {
            
            self.readyState = 4;
            var entity = response.getEntity();
            //get response
            var Util = Java.type('org.apache.http.util.EntityUtils');
            
            self.responseText = self.response = Util.toString(entity, 'UTF-8')//remove BOM marker
            //get status
            self.status = response.getStatusLine().getStatusCode();
            self.statusText = response.getStatusLine();

            //get content type
            self.responseType = 'text';
            var contentType = entity.getContentType().getValue();

            if (self._overrideMimeType) {
                contentType = self._overrideMimeType;
            }
            switch (contentType) {
                case 'application/json':
                    self.responseType = 'json';
                    break;
                default:
                    self.responseType='text'
            }
            if (self.responseType === 'json') {
                self.response = JSON.parse(self.response);
            }


            //get headers
            var headers = response.getAllHeaders();
            for (var i = 0; i < headers.length; i++) {
                self._responseHeaders[headers[i].getName()] = headers[i].getValue();
            }
            setTimeout(self.onreadystatechange, 0);
            phaser.arriveAndDeregister();
        }
}));

}

XMLHttpRequest.prototype.setRequestHeader = function (header, value) {
    this._headers[header] = value;
}