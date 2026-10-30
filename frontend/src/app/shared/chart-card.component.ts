import { Component, computed, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartType } from 'chart.js';
import { Serie } from '../core/models';
import { libelle } from '../core/labels';

const PALETTE = ['#1565c0', '#ef6c00', '#2e7d32', '#6a1b9a', '#c62828', '#00838f', '#f9a825', '#5d4037', '#455a64', '#ad1457'];

/** Carte contenant un graphique Chart.js alimente par une serie {labels, data} de l'API. */
@Component({
  selector: 'app-chart-card',
  imports: [MatCardModule, BaseChartDirective],
  template: `
    <mat-card class="card chart" appearance="outlined">
      <h3>{{ titre() }}</h3>
      @if (serie().data.length) {
        <div class="canvas"><canvas baseChart [type]="type()" [data]="data()" [options]="options()"></canvas></div>
      } @else {
        <p class="muted center">Pas encore de données</p>
      }
    </mat-card>
  `,
  styles: `
    .chart { padding: 16px 20px; height: 100%; box-sizing: border-box; }
    h3 { margin: 0 0 12px; font-size: 15px; font-weight: 500; }
    .canvas { position: relative; height: 260px; }
  `,
})
export class ChartCardComponent {
  readonly titre = input.required<string>();
  readonly serie = input.required<Serie>();
  readonly type = input<ChartType>('bar');
  readonly horizontal = input(false);

  readonly data = computed<ChartConfiguration['data']>(() => {
    const circulaire = this.type() === 'pie' || this.type() === 'doughnut';
    return {
      labels: this.serie().labels.map(libelle),
      datasets: [{
        data: this.serie().data,
        label: this.titre(),
        backgroundColor: circulaire ? PALETTE : this.type() === 'line' ? 'rgba(21,101,192,.15)' : PALETTE[0],
        borderColor: circulaire ? '#fff' : PALETTE[0],
        fill: this.type() === 'line',
        tension: 0.35,
        borderRadius: this.type() === 'bar' ? 6 : 0,
      }],
    };
  });

  readonly options = computed<ChartConfiguration['options']>(() => {
    const circulaire = this.type() === 'pie' || this.type() === 'doughnut';
    return {
      responsive: true,
      maintainAspectRatio: false,
      indexAxis: this.horizontal() ? 'y' : 'x',
      plugins: { legend: { display: circulaire, position: 'right' } },
      scales: circulaire ? {} : { x: { ticks: { precision: 0 } }, y: { beginAtZero: true, ticks: { precision: 0 } } },
    };
  });
}
