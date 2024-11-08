import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CoursService {

  private apiUrl = 'http://localhost:8085'; // Adjust this URL as needed

  // Define HTTP options with headers, e.g., for JSON
  private httpOptions = {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private http: HttpClient) {}

  // GET all plateformes
  getAllCours(): Observable<any> {
    return this.http.get<any[]>(`${this.apiUrl}/Cours`, this.httpOptions);
  }

  addCours(cours: any): Observable<string> {
    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
      }),
      responseType: 'text' as 'json', // Ensure the responseType is set to 'text' for a string response
    };
  
    return this.http.post<string>(`${this.apiUrl}/Cours`, cours, httpOptions);
  }
  
  modifyCours(id: string, cours: any): Observable<string> {
    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
      }),
      responseType: 'text' as 'json', // Ensure responseType is set to 'text' for a string response
    };
  
    // Directly return the Observable<string> without needing .pipe(map(...))
    return this.http.put<string>(`${this.apiUrl}/Cours/${id}`, cours, httpOptions);
  }
  
  deleteCours(id: string): Observable<string> {
    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
      }),
      responseType: 'text' as 'json', // This is correct for returning a string
    };
  
    return this.http.delete<string>(`${this.apiUrl}/Cours/${id}`, httpOptions);
  }
}
