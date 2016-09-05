import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';
import 'rxjs/Rx';

import { Stream } from './stream';

@Injectable()
export class StreamService {

    private dataUrl = '/v1/';
    constructor(private http: Http) {}

    public getStreamList(): Observable<Stream[]> {
        let headers = new Headers();
        headers.append('Content-Type', 'application/json');
        let options = new RequestOptions({ headers: headers });
        return this.http.get(this.dataUrl + '/streams', options)
            .map(this.extractData)
            .catch(this.handleError);
    }
    public getStream(stream: Stream): Observable<Stream> {
        let headers = new Headers();
        headers.append('Content-Type', 'application/json');
        let options = new RequestOptions({ headers: headers });
        return this.http.get(this.dataUrl + '/streams/' + stream.name, options)
            .map(this.extractData)
            .catch(this.handleError);
    }
    public saveStream(stream: Stream): Observable<Stream> {
        let body = JSON.stringify(stream);
        let headers = new Headers({'Content-Type': 'application/json'});
        let options = new RequestOptions({ headers: headers });
        return this.http.post(this.dataUrl + 'streams', body, options)
            .map(this.extractData)
            .catch(this.handleError);
    }
    public deleteStream(stream: Stream): Observable<Stream> {
        let headers = new Headers();
        headers.append('Content-Type', 'application/json');
        let options = new RequestOptions({ headers: headers });
        return this.http.delete(this.dataUrl + 'streams/' + stream.name, options)
            .map(this.extractData)
            .catch(this.handleError);
    }
    private extractData(res: Response) { //TODO Write good response parser
        let  body = {};
        if (typeof res.json()['entity']['streams'] !== 'undefined') {
            body = res.json()['entity']['streams'];
        } else if (typeof res.json()['entity']['message'] !== 'undefined') {
            body = res.json()['entity']['message'];
        } else {
            body = res.json();
        }
        return body;
    }

    private handleError(error: any) {
        let errMsg = (error._body) ? error._body :
          error.status ? `${error.status} - ${error.statusText}` : 'Server error';
        errMsg = JSON.parse(errMsg);
        let errMsgYo = errMsg.entity.message;
        return Observable.throw(errMsgYo);
    }
}
