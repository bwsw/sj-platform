import { BaseModel } from '../shared';

export class ConfigSettingModel extends BaseModel {
  name: string;
  value: string;
  domain: string;
}
