import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject, interval, of, Subject } from 'rxjs';
import { catchError, startWith, switchMap, takeUntil } from 'rxjs/operators';
import { ResumenNocDashboard } from '../modelos/modelos-operaciones';
import { OperacionesApiService } from './operaciones-api.service';

@Injectable({ providedIn: 'root' })
export class NocDashboardService implements OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly _resumen$ = new BehaviorSubject<ResumenNocDashboard | null>(null);
  private readonly _cargando$ = new BehaviorSubject<boolean>(true);
  private readonly _error$ = new BehaviorSubject<boolean>(false);
  private activo = false;

  readonly resumen$ = this._resumen$.asObservable();
  readonly cargando$ = this._cargando$.asObservable();
  readonly error$ = this._error$.asObservable();

  constructor(private readonly api: OperacionesApiService) {}

  iniciar(): void {
    if (this.activo) return;
    this.activo = true;

    interval(30_000).pipe(
      startWith(0),
      switchMap(() =>
        this.api.obtenerResumenNoc().pipe(
          catchError(() => {
            this._error$.next(true);
            this._cargando$.next(false);
            return of(null);
          })
        )
      ),
      takeUntil(this.destroy$)
    ).subscribe(resumen => {
      this._cargando$.next(false);
      if (resumen !== null) {
        this._error$.next(false);
        this._resumen$.next(resumen);
      }
    });
  }

  detener(): void {
    this.activo = false;
    this.destroy$.next();
  }

  ngOnDestroy(): void {
    this.detener();
    this.destroy$.complete();
  }
}
