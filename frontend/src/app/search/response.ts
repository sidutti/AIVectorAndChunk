import {Message} from "./message";


export class Response {
  model!: string;
  created_at: string = '';
  message!: Message;
  done_reason!: String;
  done!: boolean;
  total_duration!: number;
  load_duration!: number;
  prompt_eval_count!: number;
  prompt_eval_duration!: number;
  eval_count!: number;
  eval_duration!: number;
}
