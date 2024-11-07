import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TechnologieEducativeService {
  private apiUrl = 'http://localhost:8085'; // Base URL for the backend API

  // Define HTTP options with headers, e.g., for JSON
  private httpOptions = {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private http: HttpClient) {}

  // GET all technologies
  getAllTechnologies(): Observable<any> {
    return this.http.get(`${this.apiUrl}/getAllTechnologies`, this.httpOptions);
  }

  // POST a new technology
  addTechnologieEduc(technologie: any): Observable<string> {
    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
      }),
      responseType: 'text' as 'json', // Ensure the responseType is set to 'text' for a string response
    };

    return this.http.post<string>(`${this.apiUrl}/addTechnologieEduc`, technologie, httpOptions);
  }
}
