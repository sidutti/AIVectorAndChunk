import {HttpClient} from "@angular/common/http";
import {Injectable} from "@angular/core";

import {Observable} from "rxjs";
import {Response} from "./response";

@Injectable()
export class SearchService {
      constructor(private http: HttpClient) {
      }

      search(id: String): Observable<Response[]> {
            return this.http.post<Response[]>(`http://localhost:8080/ai/embedding/search`, id, {responseType: 'json'});
      }
}
