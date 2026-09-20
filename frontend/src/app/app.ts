import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { debounceTime, distinctUntilChanged, startWith } from 'rxjs';
import { Person, PersonImportResponse, PersonRequest } from './person.model';
import { PersonService } from './person.service';
import { translations, type Language } from './translations';
import { AuthService } from './auth/auth.service';

type SortField = 'id' | 'name' | 'age';
type SortDirection = 'asc' | 'desc';

@Component({ selector: 'app-root', imports: [CommonModule, FormsModule, ReactiveFormsModule], styleUrl: './app.scss', templateUrl: './app.html' })
export class App implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly personService = inject(PersonService);
  protected readonly auth = inject(AuthService);

  protected readonly people = signal<Person[]>([]);
  protected readonly languages: { code: Language; label: string }[] = [
    { code: 'pt', label: 'Português' }, { code: 'it', label: 'Italiano' },
    { code: 'es', label: 'Español' }, { code: 'en', label: 'English' },
    { code: 'fr', label: 'Français' }, { code: 'de', label: 'Deutsch' },
  ];
  protected readonly language = signal<Language>(this.savedLanguage());
  protected readonly t = computed(() => translations[this.language()]);
  protected readonly sortField = signal<SortField>('id');
  protected readonly sortDirection = signal<SortDirection>('asc');
  protected readonly sortedPeople = computed(() => {
    const direction = this.sortDirection() === 'asc' ? 1 : -1;
    const field = this.sortField();

    return [...this.people()].sort((left, right) => {
      let comparison: number;
      if (field === 'name') {
        comparison = `${left.firstName} ${left.lastName}`.localeCompare(
          `${right.firstName} ${right.lastName}`,
          this.language(),
          { sensitivity: 'base' },
        );
      } else {
        comparison = left[field] - right[field];
      }

      return (comparison || left.id - right.id) * direction;
    });
  });
  protected readonly currentPage = signal(1);
  protected readonly pageSize = signal(10);
  protected readonly totalPages = computed(() => Math.max(1, Math.ceil(this.sortedPeople().length / this.pageSize())));
  protected readonly paginatedPeople = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize();
    return this.sortedPeople().slice(start, start + this.pageSize());
  });
  protected readonly firstVisibleResult = computed(() => this.people().length === 0 ? 0 : (this.currentPage() - 1) * this.pageSize() + 1);
  protected readonly lastVisibleResult = computed(() => Math.min(this.currentPage() * this.pageSize(), this.people().length));
  protected readonly editingId = signal<number | null>(null);
  protected readonly selectedFile = signal<File | null>(null);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly importing = signal(false);
  protected readonly error = signal('');
  protected readonly message = signal('');
  protected readonly darkMode = signal(localStorage.getItem('theme') !== 'light');
  protected readonly searchControl = this.formBuilder.nonNullable.control('');
  protected readonly personForm = this.formBuilder.group({
    firstName: this.formBuilder.nonNullable.control('', Validators.required),
    lastName: this.formBuilder.nonNullable.control('', Validators.required),
    age: this.formBuilder.nonNullable.control(1, [Validators.required, Validators.min(1)]),
    addressId: this.formBuilder.control<number | null>(null),
  });

  ngOnInit(): void {
    this.applyTheme();
    const userLanguage = this.languageFromUserLocale();
    if (userLanguage) {
      this.applyLanguage(userLanguage);
    } else {
      document.documentElement.lang = this.language();
    }
    if (!this.auth.authenticated()) {
      return;
    }
    this.searchControl.valueChanges.pipe(startWith(''), debounceTime(300), distinctUntilChanged())
      .subscribe((term) => this.loadPeople(term));
  }

  protected submit(): void {
    if (this.personForm.invalid) { this.personForm.markAllAsTouched(); return; }
    if (this.editingId() === null && !this.auth.hasRole('create_users')) return;
    if (this.editingId() !== null && !this.auth.hasRole('edit_users')) return;
    this.saving.set(true);
    this.clearFeedback();
    const request = this.personForm.getRawValue() as PersonRequest;
    const id = this.editingId();
    const operation = id === null ? this.personService.create(request) : this.personService.update(id, request);
    operation.subscribe({
      next: () => {
        this.message.set(id === null ? this.t().created : this.t().updated);
        this.cancelEdit();
        this.loadPeople(this.searchControl.value);
      },
      error: (error) => { this.error.set(this.errorMessage(error)); this.saving.set(false); },
      complete: () => this.saving.set(false),
    });
  }

  protected edit(id: number): void {
    if (!this.auth.hasRole('edit_users')) return;
    this.clearFeedback();
    this.personService.getById(id).subscribe({
      next: (person) => {
        this.editingId.set(person.id);
        this.personForm.setValue({ firstName: person.firstName, lastName: person.lastName, age: person.age, addressId: person.addressId ?? null });
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error: (error) => this.error.set(this.errorMessage(error)),
    });
  }

  protected remove(person: Person): void {
    if (!this.auth.hasRole('delete_users')) return;
    if (!confirm(`${this.t().deleteConfirm} ${person.firstName} ${person.lastName}?`)) return;
    this.clearFeedback();
    this.personService.delete(person.id).subscribe({
      next: () => { this.message.set(this.t().deleted); this.loadPeople(this.searchControl.value); },
      error: (error) => this.error.set(this.errorMessage(error)),
    });
  }

  protected cancelEdit(): void {
    this.editingId.set(null);
    this.personForm.reset({ firstName: '', lastName: '', age: 1, addressId: null });
  }

  protected chooseFile(event: Event): void {
    this.selectedFile.set((event.target as HTMLInputElement).files?.[0] ?? null);
  }

  protected importCsv(): void {
    if (!this.auth.hasRole('import_users')) return;
    const file = this.selectedFile();
    if (!file) { this.error.set(this.t().selectCsv); return; }
    this.importing.set(true);
    this.clearFeedback();
    this.personService.importCsv(file).subscribe({
      next: (result: PersonImportResponse) => {
        this.message.set(`${this.t().importComplete}: ${result.written} ${this.t().saved}, ${result.ignored} ${this.t().ignored} ${this.t().of} ${result.read} ${this.t().read}.`);
        this.selectedFile.set(null);
        this.loadPeople(this.searchControl.value);
      },
      error: (error) => { this.error.set(this.errorMessage(error)); this.importing.set(false); },
      complete: () => this.importing.set(false),
    });
  }

  protected toggleTheme(): void {
    this.darkMode.update((dark) => !dark);
    localStorage.setItem('theme', this.darkMode() ? 'dark' : 'light');
    this.applyTheme();
  }

  protected changeLanguage(language: string): void {
    if (!this.isSupportedLanguage(language)) {
      return;
    }
    this.applyLanguage(language);
    this.clearFeedback();
  }

  protected sortBy(field: SortField): void {
    if (this.sortField() === field) {
      this.sortDirection.update((direction) => direction === 'asc' ? 'desc' : 'asc');
      this.currentPage.set(1);
      return;
    }

    this.sortField.set(field);
    this.sortDirection.set('asc');
    this.currentPage.set(1);
  }

  protected sortIndicator(field: SortField): string {
    if (this.sortField() !== field) return '';
    return this.sortDirection() === 'asc' ? '↑' : '↓';
  }

  protected searchPlaceholder(): string {
    return {
      pt: 'Pesquisar por nome, apelido ou idade…',
      it: 'Cerca per nome, cognome o età…',
      es: 'Buscar por nombre, apellido o edad…',
      en: 'Search by first name, last name or age…',
      fr: 'Rechercher par prénom, nom ou âge…',
      de: 'Nach Vorname, Nachname oder Alter suchen…',
    }[this.language()];
  }

  protected changePageSize(event: Event): void {
    this.pageSize.set(Number((event.target as HTMLSelectElement).value));
    this.currentPage.set(1);
  }

  protected previousPage(): void {
    this.currentPage.update((page) => Math.max(1, page - 1));
  }

  protected nextPage(): void {
    this.currentPage.update((page) => Math.min(this.totalPages(), page + 1));
  }

  private loadPeople(term: string): void {
    this.loading.set(true);
    const request = term.trim() ? this.personService.search(term.trim()) : this.personService.list();
    request.subscribe({
      next: (people) => {
        this.people.set(people);
        this.currentPage.set(1);
      },
      error: (error) => { this.error.set(this.errorMessage(error)); this.loading.set(false); },
      complete: () => this.loading.set(false),
    });
  }

  private clearFeedback(): void { this.error.set(''); this.message.set(''); }
  private applyLanguage(language: Language): void {
    this.language.set(language);
    localStorage.setItem('language', language);
    document.documentElement.lang = language;
  }
  private languageFromUserLocale(): Language | null {
    if (!this.auth.authenticated() || typeof this.auth.locale !== 'string') {
      return null;
    }

    const language = this.auth.locale.toLowerCase().replace('_', '-').split('-')[0];
    return this.isSupportedLanguage(language) ? language : null;
  }
  private isSupportedLanguage(language: string): language is Language {
    return language in translations;
  }
  private errorMessage(error: { error?: { detail?: string }; message?: string }): string {
    return error.error?.detail ?? error.message ?? this.t().errorDefault;
  }
  private applyTheme(): void { document.documentElement.dataset['theme'] = this.darkMode() ? 'dark' : 'light'; }
  private savedLanguage(): Language {
    const language = localStorage.getItem('language');
    return language && language in translations ? language as Language : 'pt';
  }
}
