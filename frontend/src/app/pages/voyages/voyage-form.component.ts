import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { startWith } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { AiService, UserService, VoyageService } from '../../core/api.service';
import { NotifyService } from '../../core/notify.service';
import { DOMAINE_KEYS, DOMAINES, fromIsoDate, OBJET_KEYS, OBJETS, toIsoDate } from '../../core/labels';
import { DomaineActivite, ObjetVoyage, User, Ville, VoyageRequest } from '../../core/models';
import { SHARED } from '../../shared/shared';

/** Ajout / modification d'un voyage. A l'enregistrement, le backend lance l'algorithme de matching. */
@Component({
  selector: 'app-voyage-form',
  imports: [SHARED],
  templateUrl: './voyage-form.component.html',
})
export class VoyageFormComponent implements OnInit {
  auth = inject(AuthService);
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private voyageService = inject(VoyageService);
  private userService = inject(UserService);
  private notify = inject(NotifyService);

  /** Parametre de route :id (absent en creation). */
  readonly id = input<string>();

  readonly domaines = DOMAINE_KEYS;
  readonly libellesDomaines = DOMAINES;
  readonly objets = OBJET_KEYS;
  readonly libellesObjets = OBJETS;
  readonly aujourdhui = new Date();

  readonly villes = signal<Ville[]>([]);
  readonly employes = signal<User[]>([]);
  readonly loading = signal(false);
  readonly generation = signal(false);
  private ai = inject(AiService);

  form = this.fb.group({
    titre: ['', [Validators.required, Validators.maxLength(150)]],
    ville: ['', Validators.required],
    paysDestination: ['', Validators.required],
    latitude: [null as number | null, Validators.required],
    longitude: [null as number | null, Validators.required],
    rayonKm: [50],
    dateDebut: [null as Date | null, Validators.required],
    dateFin: [null as Date | null, Validators.required],
    objet: [null as ObjetVoyage | null, Validators.required],
    description: [''],
    domainesRecherches: [[] as DomaineActivite[]],
    visible: [true],
    employeId: [null as number | null],
  });

  private readonly saisieVille = toSignal(this.form.controls.ville.valueChanges.pipe(startWith('')), { initialValue: '' });

  readonly villesFiltrees = computed(() => {
    const q = (this.saisieVille() ?? '').toLowerCase();
    return this.villes().filter(v => `${v.nom} ${v.pays}`.toLowerCase().includes(q)).slice(0, 30);
  });

  get enEdition(): boolean {
    return !!this.id();
  }

  ngOnInit(): void {
    this.voyageService.villes().subscribe(v => this.villes.set(v));
    if (this.auth.hasRole('ENTREPRISE')) {
      this.form.controls.employeId.addValidators(Validators.required);
      this.userService.employes().subscribe(e => this.employes.set(e.filter(x => x.actif)));
    }
    const id = this.id();
    if (id) {
      this.voyageService.get(+id).subscribe({
        next: v => this.form.patchValue({
          titre: v.titre,
          ville: v.villeDestination,
          paysDestination: v.paysDestination,
          latitude: v.latitude,
          longitude: v.longitude,
          rayonKm: v.rayonKm,
          dateDebut: fromIsoDate(v.dateDebut),
          dateFin: fromIsoDate(v.dateFin),
          objet: v.objet,
          description: v.description ?? '',
          domainesRecherches: v.domainesRecherches,
          visible: v.visible,
          employeId: v.employeId,
        }),
        error: err => this.notify.error(err),
      });
    }
  }

  /** Une ville du referentiel fournit pays et coordonnees GPS utilisees par le matching. */
  choisirVille(e: MatAutocompleteSelectedEvent): void {
    const v = this.villes().find(x => x.nom === e.option.value);
    if (v) {
      this.form.patchValue({ paysDestination: v.pays, latitude: v.latitude, longitude: v.longitude });
    }
  }

  /** Si l'utilisateur modifie la ville a la main, les coordonnees ne sont plus valides. */
  villeSaisie(): void {
    const nom = this.form.controls.ville.value;
    const v = this.villes().find(x => x.nom.toLowerCase() === (nom ?? '').toLowerCase());
    if (v) {
      this.form.patchValue({ ville: v.nom, paysDestination: v.pays, latitude: v.latitude, longitude: v.longitude });
    } else {
      this.form.patchValue({ latitude: null, longitude: null });
    }
  }

  /** Demande a l'IA un programme jour par jour a partir des champs deja remplis. */
  genererProgramme(): void {
    const f = this.form.getRawValue();
    if (!f.titre || !f.ville || !f.dateDebut || !f.dateFin || !f.objet) {
      this.notify.error(null, 'Renseignez le titre, la ville, les dates et l\'objet avant de générer le programme');
      return;
    }
    this.generation.set(true);
    this.ai.programme({
      titre: f.titre,
      villeDestination: f.ville,
      paysDestination: f.paysDestination ?? '',
      dateDebut: toIsoDate(f.dateDebut),
      dateFin: toIsoDate(f.dateFin),
      objet: f.objet,
      domainesRecherches: f.domainesRecherches ?? [],
    }).subscribe({
      next: r => {
        this.form.patchValue({ description: r.texte.slice(0, 2000) });
        this.generation.set(false);
        this.notify.success('Programme généré : vous pouvez le modifier avant d\'enregistrer');
      },
      error: err => {
        this.generation.set(false);
        this.notify.error(err);
      },
    });
  }

  formatKm(v: number): string {
    return `${v} km`;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      if (this.form.controls.latitude.invalid) {
        this.notify.error(null, 'Choisissez la ville de destination dans la liste proposée');
      }
      return;
    }
    const f = this.form.getRawValue();
    const req: VoyageRequest = {
      titre: f.titre!,
      villeDestination: f.ville!,
      paysDestination: f.paysDestination!,
      latitude: f.latitude!,
      longitude: f.longitude!,
      rayonKm: f.rayonKm ?? 50,
      dateDebut: toIsoDate(f.dateDebut!),
      dateFin: toIsoDate(f.dateFin!),
      objet: f.objet!,
      description: f.description,
      domainesRecherches: f.domainesRecherches ?? [],
      visible: f.visible ?? true,
      employeId: f.employeId,
    };
    this.loading.set(true);
    const id = this.id();
    const appel = id ? this.voyageService.modifier(+id, req) : this.voyageService.creer(req);
    appel.subscribe({
      next: res => {
        const n = res.nouveauxMatchs;
        this.notify.success(`Voyage ${id ? 'modifié' : 'enregistré'} — `
          + (n ? `${n} partenaire(s) de voyage trouvé(s) et invité(s) !` : 'aucun nouveau partenaire pour le moment.'));
        this.router.navigate(['/voyages', res.voyage.id]);
      },
      error: err => {
        this.loading.set(false);
        this.notify.error(err);
      },
    });
  }
}
