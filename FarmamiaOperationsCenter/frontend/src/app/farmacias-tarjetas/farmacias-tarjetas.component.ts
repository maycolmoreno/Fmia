import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { CatalogoRegion, TarjetaEquipo } from '../modelos/modelos-operaciones';
import { OperacionesApiService } from '../servicios/operaciones-api.service';

@Component({
  selector: 'app-farmacias-tarjetas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './farmacias-tarjetas.component.html',
  styleUrl: './farmacias-tarjetas.component.css'
})
export class FarmaciasTarjetasComponent implements OnInit {
  regionesCatalogo: CatalogoRegion[] = [];
  provinciasDisponibles: string[] = [];
  tarjetas: TarjetaEquipo[] = [];

  regionSeleccionada = '';
  provinciaSeleccionada = '';
  cargandoCatalogo = false;
  cargandoTarjetas = false;
  error = '';

  constructor(private readonly api: OperacionesApiService) {}

  ngOnInit(): void {
    this.cargarCatalogoRegiones();
  }

  cargarCatalogoRegiones(): void {
    this.cargandoCatalogo = true;
    this.error = '';

    this.api.getRegionesCatalogo()
      .pipe(finalize(() => this.cargandoCatalogo = false))
      .subscribe({
        next: (catalogo) => {
          this.regionesCatalogo = catalogo;
          this.provinciasDisponibles = [];
        },
        error: (respuesta) => {
          this.error = respuesta?.error?.message ?? 'No se pudo cargar el catalogo geografico.';
        }
      });
  }

  seleccionarRegion(region: string): void {
    this.regionSeleccionada = region;
    this.provinciaSeleccionada = '';
    this.tarjetas = [];

    const seleccion = this.regionesCatalogo.find((item) => item.region === region);
    this.provinciasDisponibles = seleccion?.provincias ?? [];
  }

  seleccionarProvincia(provincia: string): void {
    this.provinciaSeleccionada = provincia;
    this.tarjetas = [];

    if (!provincia) {
      return;
    }

    this.cargarTarjetas(provincia);
  }

  cargarTarjetas(provincia: string): void {
    this.cargandoTarjetas = true;
    this.error = '';

    forkJoin({
      tarjetas: this.api.getTarjetasEquipos(provincia),
      estados: this.api.getEstadosMonitoreoEquipos()
    })
      .pipe(finalize(() => this.cargandoTarjetas = false))
      .subscribe({
        next: ({ tarjetas, estados }) => {
          const estadoPorEquipo = new Map(estados.map((estado) => [estado.idEquipo, estado.online]));
          this.tarjetas = tarjetas.map((tarjeta) => ({
            ...tarjeta,
            isOnline: estadoPorEquipo.get(tarjeta.idEquipo) ?? false
          }));
        },
        error: (respuesta) => {
          this.error = respuesta?.error?.message ?? 'No se pudieron cargar las tarjetas de equipos.';
        }
      });
  }

  trackRegion(_: number, item: CatalogoRegion): string {
    return item.region;
  }

  trackProvincia(_: number, provincia: string): string {
    return provincia;
  }

  trackTarjeta(_: number, tarjeta: TarjetaEquipo): string {
    return tarjeta.idEquipo;
  }
}
