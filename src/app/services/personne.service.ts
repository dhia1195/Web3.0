import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
@Injectable({
  providedIn: 'root'
})
export class PersonneService {

  private apiUrl = 'http://localhost:8085/personne'; // Remplacez par votre URL d'API

  constructor(private http: HttpClient) {}

  // Méthode pour obtenir la liste des personnes
  getPersonnes(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  

  // Méthode pour ajouter une nouvelle personne
  addPersonne(personne: any): Observable<any> {
    return this.http.post<any>(this.apiUrl, personne);
  }

  // Méthode pour mettre à jour une personne existante
  updatePersonne(id: any, personne: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, personne);
  }

  // Méthode pour supprimer une personne
  deletePersonne(id: string): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`);
  }
}
