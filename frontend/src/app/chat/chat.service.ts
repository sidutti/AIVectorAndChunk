import {HttpClient, HttpHeaders} from "@angular/common/http";
import {Injectable} from "@angular/core";

import {Observable} from "rxjs";
import {ChatData} from "./chatData";


@Injectable()
export class ChatService {
      constructor(private http: HttpClient) {
      }

      search(id: string): Observable<ChatData[]> {
            const headers = new HttpHeaders();
            headers.set('Content-Type', 'application/json');
            return this.http.post<ChatData[]>(`http://localhost:8080/ai/chat`, id, {headers: headers});
      }
}
