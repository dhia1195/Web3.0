import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { map, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class MethodeService {
  private apiUrl = 'http://localhost:8085'; // Adjust this URL as needed

  // Define HTTP options with headers, e.g., for JSON
  private httpOptions = {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private http: HttpClient) {}

  // GET all plateformes
  getAllMethodes(): Observable<any> {
    return this.http.get(`${this.apiUrl}/methodes`, this.httpOptions);
  }

  addMethode(methode: any): Observable<string> {
    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
      }),
      responseType: 'text' as 'json', // Ensure the responseType is set to 'text' for a string response
    };
  
    return this.http.post<string>(`${this.apiUrl}/addMethode`, methode, httpOptions);
  }
  
  modifyMethode(id: string, methode: any): Observable<string> {
    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
      }),
      responseType: 'text' as 'json', // Ensure responseType is set to 'text' for a string response
    };
  
    // Directly return the Observable<string> without needing .pipe(map(...))
    return this.http.put<string>(`${this.apiUrl}/modifyMethode/${id}`, methode, httpOptions);
  }
  
  deleteMethode(id: string): Observable<string> {
    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
      }),
      responseType: 'text' as 'json', // This is correct for returning a string
    };
  
    return this.http.delete<string>(`${this.apiUrl}/deleteMethode/${id}`, httpOptions);
  }
  
}
