import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Person, PersonImportResponse, PersonRequest } from './person.model';

@Injectable({ providedIn: 'root' })
export class PersonService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/persons';
  list(): Observable<Person[]> { return this.http.get<Person[]>(this.baseUrl); }
  search(query: string): Observable<Person[]> {
    return this.http.get<Person[]>(`${this.baseUrl}/search`, { params: new HttpParams().set('query', query) });
  }
  getById(id: number): Observable<Person> { return this.http.get<Person>(`${this.baseUrl}/${id}`); }
  create(person: PersonRequest): Observable<Person> { return this.http.post<Person>(this.baseUrl, person); }
  update(id: number, person: PersonRequest): Observable<Person> { return this.http.put<Person>(`${this.baseUrl}/${id}`, person); }
  delete(id: number): Observable<void> { return this.http.delete<void>(`${this.baseUrl}/${id}`); }
  importCsv(file: File): Observable<PersonImportResponse> {
    const body = new FormData();
    body.append('file', file);
    return this.http.post<PersonImportResponse>(`${this.baseUrl}/import`, body);
  }
}
