import { Observable } from 'rxjs/Rx';
import { BaseService, IResponse } from '../index';
import { BaseModel } from '../index';

export abstract class CrudService<M extends BaseModel> extends BaseService<M>  {

  public save(model: M): Observable<IResponse<M>> {
    return this.post(model);
  };

  protected post(model: M): Observable<IResponse<M>> {
    return this.http
      .post(this.requestUrl, model, this.getRequestOptions())
      .map(this.extractData)
      .catch(this.handleError);
  }

  public remove(name: string): Observable<IResponse<M>> {
    return this.http
      .delete(`${this.requestUrl}/${name}`, this.getRequestOptions())
      .map(this.extractData)
      .catch(this.handleError);
  }

}
