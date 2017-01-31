import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { StreamModel } from '../models/stream.model';

@Injectable()
export class StreamsService {
  private _dataUrl = '/v1/';

  constructor(private http: Http) {
  }

  public getStreamList(): Observable<StreamModel[]> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.get(this._dataUrl + 'streams', options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  public getStreamTypes(): Observable<string[]> {
    return this.http.get(this._dataUrl + 'streams/types')
      .map(this.extractData)
      .catch(this.handleError);
  }

  public getRelatedInstancesList(streamName: string): Observable<string[]> {
    return this.http.get(this._dataUrl + 'streams/' + streamName + '/related')
      .map(this.extractData)
      .catch(this.handleError);
  }

  public getStream(stream: StreamModel): Observable<StreamModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.get(this._dataUrl + 'streams/' + stream.name, options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  public saveStream(stream: StreamModel): Observable<StreamModel> {
    let body = JSON.stringify(stream);
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });
    return this.http.post(this._dataUrl + 'streams', body, options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  public deleteStream(stream: StreamModel): Observable<StreamModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.delete(this._dataUrl + 'streams/' + stream.name, options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  private extractData(res: Response) { //TODO Write good response parser
    let body = {};
    if (typeof res.json()['entity']['streams'] !== 'undefined') {
      body = res.json()['entity']['streams'];
    } else if (typeof res.json()['entity']['message'] !== 'undefined') {
      body = res.json()['entity']['message'];
    } else if (typeof res.json()['entity']['types'] !== 'undefined') {
      body = res.json()['entity']['types'];
    } else if (typeof res.json()['entity']['instances'] !== 'undefined') {
      body = res.json()['entity']['instances'];
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
