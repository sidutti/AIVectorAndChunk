import {Component} from "@angular/core";
import {SearchService} from "./search.service";
import {MatCard, MatCardContent} from "@angular/material/card";
import {MatFormField, MatLabel, MatSuffix} from "@angular/material/form-field";
import {FormsModule} from "@angular/forms";
import {MatInput} from "@angular/material/input";
import {MatButton} from "@angular/material/button";
import {
      MatCell,
      MatCellDef,
      MatColumnDef,
      MatHeaderCell,
      MatHeaderCellDef,
      MatHeaderRow,
      MatHeaderRowDef,
      MatRow,
      MatRowDef,
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
      stringValue: string = '';
      value: string = '';
      displayedColumns: string[] = ['Search-Accuracy', 'Response'];

      columnsToDisplayWithExpand = [...this.displayedColumns, 'expand'];
      expandedElement: Response | null | undefined;
      element: Response | null | undefined;

      constructor(private searchService: SearchService) {
      }

      generate(context: Response | null | undefined): void {
            this.stringValue = '';
            if (context?.content) {
                  this.searchService.rag(this.value +'###'+context.content)
                        .subscribe(result => {
                              let item;
                              for (item of result) {
                                    this.stringValue += item.data;
                              }
                              context.id=this.stringValue;
                        });
            }
      }

      search(searchString: string) {

            this.searchService.search(searchString)
                  .subscribe(res => this.data = res);
      }


}
