import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { StreamModel } from '../models/stream.model';
import { BaseModel } from '../models/base.model';
import { BaseService, BService } from './base.service';

@Injectable()
@BService({
  entity: 'streams',
  entityModel: StreamModel
})
export class StreamsService extends BaseService<StreamModel> {
  private dataUrl = '/v1/';

  public getRelatedInstancesList(streamName: string): Observable<string[]> {
    return this.http.get(this.dataUrl + 'streams/' + streamName + '/related')
      .map(response => {
        const data = this.extractData(response);
        return data.instances;
      })
      .catch(this.handleError);
  }

  public getStream(stream: StreamModel): Observable<StreamModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.get(this.dataUrl + 'streams/' + stream.name, options)
      .map(response => {
        const data = this.extractData(response);
        return data.streams;
      })
      .catch(this.handleError);
  }
}
