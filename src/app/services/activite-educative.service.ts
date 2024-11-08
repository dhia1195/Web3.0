import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ActiviteEducativeService {


  private apiUrl = 'http://localhost:8085'; // Adjust this URL as needed

  // Define HTTP options with headers, e.g., for JSON
  private httpOptions = {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private http: HttpClient) {}


  // GET all plateformes
  getAllActivite(): Observable<any> {


    return this.http.get<any[]>(`${this.apiUrl}/activite_educative`, this.httpOptions);
  }

  addCours(activite_educative: any): Observable<string> {
    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
      }),
      responseType: 'text' as 'json', // Ensure the responseType is set to 'text' for a string response
    };
  
    return this.http.post<string>(`${this.apiUrl}/activite_educative`, activite_educative, httpOptions);
  }
  
  modifyactivite_educative(id: string, activite_educative: any): Observable<string> {
    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
      }),
      responseType: 'text' as 'json', // Ensure responseType is set to 'text' for a string response
    };
  
    // Directly return the Observable<string> without needing .pipe(map(...))
    return this.http.put<string>(`${this.apiUrl}/activite_educative/${id}`, activite_educative, httpOptions);
  }
  
  deleteactivite_educative(id: string): Observable<string> {
    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
      }),
      responseType: 'text' as 'json', // This is correct for returning a string
    };
  
    return this.http.delete<string>(`${this.apiUrl}/activite_educative/${id}`, httpOptions);
  }
}
