import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';
import 'rxjs/Rx';

import { Service } from './service';

@Injectable()
export class ServiceService {

    private dataUrl = '/v1/';
    constructor(private http: Http) {}

    public getServiceList(): Observable<Service[]> {
        let headers = new Headers();
        headers.append('Content-Type', 'application/json');
        let options = new RequestOptions({ headers: headers });
        return this.http.get(this.dataUrl + 'services', options)
            .map(this.extractData)
            .catch(this.handleError);
    }
    public getService(serviceName: string): Observable<Service> {
        let headers = new Headers();
        headers.append('Content-Type', 'application/json');
        let options = new RequestOptions({ headers: headers });
        return this.http.get(this.dataUrl + 'services/' + serviceName, options)
            .map(this.extractData)
            .catch(this.handleError);
    }
    public deleteService(service: Service): Observable<Service> {
        let headers = new Headers();
        headers.append('Content-Type', 'application/json');
        let options = new RequestOptions({ headers: headers });
        return this.http.delete(this.dataUrl + 'services/' + service.name, options)
            .map(this.extractData)
            .catch(this.handleError);
    }
    public saveService(service: Service): Observable<Service> {
        let body = JSON.stringify(service);
        let headers = new Headers({'Content-Type': 'application/json'});
        let options = new RequestOptions({ headers: headers });
        return this.http.post(this.dataUrl + 'services', body, options)
            .map(this.extractData)
            .catch(this.handleError);
    }
    private extractData(res: Response) { //TODO Write good response parser
        let  body = {};
        if (typeof res.json()['entity']['services'] !== 'undefined') {
            body = res.json()['entity']['services'];
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
