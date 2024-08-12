import {HttpClient} from "@angular/common/http";
import {Injectable} from "@angular/core";

import {Observable} from "rxjs";


@Injectable()
export class ChatService {
      constructor(private http: HttpClient) {
      }

      search(id: string): Observable<string> {
            return this.http.post(`http://localhost:8080/ai/chat`,   id,  { responseType: 'text' });
      }
}
