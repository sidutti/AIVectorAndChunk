import {HttpClient} from "@angular/common/http";
import {Injectable} from "@angular/core";

import {Observable} from "rxjs";
import {Questions} from "./question";

@Injectable()
export class SearchService {
  constructor(private http: HttpClient) {
  }

  search(id: Questions): Observable<string> {
    return this.http.post(`http://localhost:11434/api/chat`, id, {responseType: 'text'});
  }
}
