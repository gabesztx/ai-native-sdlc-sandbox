import { Me } from '../../core/api/models';

export interface StoredAuth {
  accessToken: string;
  refreshToken: string;
  me: Me;
}
