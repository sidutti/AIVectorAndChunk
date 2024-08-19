import {Component} from "@angular/core";
import {SearchService} from "./search.service";
import {MatCard, MatCardContent} from "@angular/material/card";
import {MatFormField, MatSuffix} from "@angular/material/form-field";
import {FormsModule} from "@angular/forms";
import {MatInput} from "@angular/material/input";
import {MatButton} from "@angular/material/button";
import {MatLabel} from "@angular/material/form-field";
import {
      MatCell,
      MatCellDef,
      MatColumnDef,
      MatHeaderCell,
      MatHeaderCellDef,
      MatHeaderRow, MatHeaderRowDef, MatRow, MatRowDef,
      MatTable
} from "@angular/material/table";
import {Response} from "./response";
import {MatProgressSpinner} from "@angular/material/progress-spinner";
import {NgIf} from "@angular/common";


@Component({
      selector: 'app-search',
      standalone: true,
      imports: [
            MatLabel,
            MatCard,
            MatCardContent,
            MatFormField,
            FormsModule,
            MatInput,
            MatButton,
            MatTable,
            MatColumnDef,
            MatHeaderCell,
            MatCell,
            MatHeaderCellDef,
            MatCellDef,
            MatHeaderRow,
            MatRow,
            MatRowDef,
            MatHeaderRowDef,
            MatProgressSpinner,
            NgIf,
            MatSuffix
      ],
      providers: [SearchService],
      templateUrl: './search.component.html',
      styleUrl: './search.component.css'
})
export class SearchComponent {
      data: Response[] = [];
      value:string = '';
      displayedColumns: string[] = ['Search-Accuracy', 'Response'];
      constructor(private searchService: SearchService) {
      }

      search(searchString: string) {

            this.searchService.search(searchString)
                  .subscribe(res => this.data= res);
      }


}
