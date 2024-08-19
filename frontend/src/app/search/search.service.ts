import {HttpClient} from "@angular/common/http";
import {Injectable} from "@angular/core";

import {Observable} from "rxjs";
import {Response} from "./response";
import {ChatData} from "../chat/chatData";

@Injectable()
export class SearchService {
      constructor(private http: HttpClient) {
      }

      search(id: String): Observable<Response[]> {
            return this.http.post<Response[]>(`http://localhost:8080/ai/embedding/search`, id, {responseType: 'json'});
      }

      rag(context: string) {
            return this.http.post<ChatData[]>(`http://localhost:8080/ai/rag/generate`, context, {responseType: 'json'});
      }
}
