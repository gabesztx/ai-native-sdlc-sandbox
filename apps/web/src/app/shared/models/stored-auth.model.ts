import { Me } from '../../api/models';

export interface StoredAuth {
  accessToken: string;
  refreshToken: string;
  me: Me;
}
