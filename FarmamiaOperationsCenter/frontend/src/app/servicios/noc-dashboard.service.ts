import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject, forkJoin, interval, of, Subject } from 'rxjs';
import { catchError, startWith, switchMap, takeUntil } from 'rxjs/operators';
import { ContadoresEnlaces, EstadoOperacionalFarmacia, ResumenNocDashboard } from '../modelos/modelos-operaciones';
import { OperacionesApiService } from './operaciones-api.service';

@Injectable({ providedIn: 'root' })
export class NocDashboardService implements OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly _resumen$ = new BehaviorSubject<ResumenNocDashboard | null>(null);
  private readonly _estadoFarmacias$ = new BehaviorSubject<EstadoOperacionalFarmacia[]>([]);
  private readonly _contadoresEnlaces$ = new BehaviorSubject<ContadoresEnlaces>({ total: 0, up: 0, down: 0 });
  private readonly _cargando$ = new BehaviorSubject<boolean>(true);
  private readonly _error$ = new BehaviorSubject<boolean>(false);
  private activo = false;

  readonly resumen$ = this._resumen$.asObservable();
  readonly estadoFarmacias$ = this._estadoFarmacias$.asObservable();
  readonly contadoresEnlaces$ = this._contadoresEnlaces$.asObservable();
  readonly cargando$ = this._cargando$.asObservable();
  readonly error$ = this._error$.asObservable();

  constructor(private readonly api: OperacionesApiService) {}

  iniciar(): void {
    this.iniciarRefresco();
  }

  iniciarRefresco(): void {
    if (this.activo) return;
    this.activo = true;

    interval(30_000).pipe(
      startWith(0),
      switchMap(() =>
        forkJoin({
          resumen: this.api.obtenerResumenNoc().pipe(catchError(() => of(null))),
          estadoFarmacias: this.api.listarEstadoFarmacias().pipe(catchError(() => of(null))),
          contadoresEnlaces: this.api.obtenerContadoresEnlaces().pipe(catchError(() => of(null)))
        })
      ),
      takeUntil(this.destroy$)
    ).subscribe(({ resumen, estadoFarmacias, contadoresEnlaces }) => {
      this._cargando$.next(false);
      if (resumen === null && estadoFarmacias === null && contadoresEnlaces === null) {
        this._error$.next(true);
        return;
      }
      this._error$.next(false);
      if (resumen !== null) {
        this._resumen$.next(resumen);
      }
      if (estadoFarmacias !== null) {
        this._estadoFarmacias$.next(estadoFarmacias);
      }
      if (contadoresEnlaces !== null) {
        this._contadoresEnlaces$.next(contadoresEnlaces);
      }
    });
  }

  detener(): void {
    this.detenerRefresco();
  }

  detenerRefresco(): void {
    this.activo = false;
    this.destroy$.next();
  }

  ngOnDestroy(): void {
    this.detener();
    this.destroy$.complete();
  }
}
