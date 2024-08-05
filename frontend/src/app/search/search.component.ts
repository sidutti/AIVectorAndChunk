import {Component} from "@angular/core";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatInputModule} from "@angular/material/input";
import {FormsModule} from "@angular/forms";
import {MatButtonModule} from "@angular/material/button";
import {MatIconModule} from "@angular/material/icon";
import {MatDividerModule} from "@angular/material/divider";
import {SearchService} from "./search.service";
import {Questions} from "./question";
import {Message} from "./message";
import {Response} from "./response";
import {NgForOf} from "@angular/common";

@Component({
      selector: 'app-search',
      standalone: true,
      imports: [MatFormFieldModule, MatInputModule, FormsModule, MatButtonModule, MatIconModule, MatDividerModule, NgForOf],
      providers: [SearchService],
      templateUrl: './search.component.html',
      styleUrl: './search.component.css'
})
export class SearchComponent {
      res: Response[] = [];
      value = '';

      constructor(private searchService: SearchService) {
      }

      search(searchString: string) {
            const q = new Questions();
            const message = new Message();
            message.role = 'user';
            message.content = searchString;
            q.messages.push(message);
            this.searchService.search(searchString)
                  .subscribe(res => (this.res.push(res)));
      }


}
