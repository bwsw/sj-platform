import { BaseModel } from './base.model';

export class SettingModel extends BaseModel {
  name: string;
  value: string;
  domain: string;
}
