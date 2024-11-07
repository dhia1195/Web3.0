// plateforme.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { map, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PlateformeService {
  private apiUrl = 'http://localhost:8085'; // Adjust this URL as needed

  // Define HTTP options with headers, e.g., for JSON
  private httpOptions = {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private http: HttpClient) {}

  // GET all plateformes
  getAllPlateformes(): Observable<any> {
    return this.http.get(`${this.apiUrl}/plateformes`, this.httpOptions);
  }

  addPlateforme(plateforme: any): Observable<string> {
    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
      }),
      responseType: 'text' as 'json', // Ensure the responseType is set to 'text' for a string response
    };
  
    return this.http.post<string>(`${this.apiUrl}/addPlateforme`, plateforme, httpOptions);
  }
  
  modifyPlateforme(id: string, plateforme: any): Observable<string> {
    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
      }),
      responseType: 'text' as 'json', // Ensure responseType is set to 'text' for a string response
    };
  
    // Directly return the Observable<string> without needing .pipe(map(...))
    return this.http.put<string>(`${this.apiUrl}/modifyPlateforme/${id}`, plateforme, httpOptions);
  }
  
  deletePlateforme(id: string): Observable<string> {
    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
      }),
      responseType: 'text' as 'json', // This is correct for returning a string
    };
  
    return this.http.delete<string>(`${this.apiUrl}/delete/${id}`, httpOptions);
  }
  
}
