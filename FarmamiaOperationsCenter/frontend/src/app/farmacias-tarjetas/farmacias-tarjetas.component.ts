import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { CatalogoRegion, TarjetaEquipo } from '../modelos/modelos-operaciones';
import { OperacionesApiService } from '../servicios/operaciones-api.service';

@Component({
  selector: 'app-farmacias-tarjetas',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './farmacias-tarjetas.component.html',
  styleUrl: './farmacias-tarjetas.component.css'
})
export class FarmaciasTarjetasComponent implements OnInit {
  regionesCatalogo: CatalogoRegion[] = [];
  provinciasDisponibles: string[] = [];
  tarjetas: TarjetaEquipo[] = [];

  regionSeleccionada = '';
  provinciaSeleccionada = '';
  filtroBusqueda = '';
  cargandoCatalogo = false;
  cargandoTarjetas = false;
  modalRegistroAbierto = false;
  guardandoEquipo = false;
  error = '';
  mensaje = '';

  readonly formularioRegistro = this.fb.nonNullable.group({
    codigoPdv: ['', [Validators.required, Validators.pattern(/^[A-Z]{2}\d{3}$/)]],
    direccionIp: ['', [Validators.required, Validators.pattern(/^(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}$/)]],
    comunidadSnmp: ['public', Validators.required]
  });

  constructor(
    private readonly api: OperacionesApiService,
    private readonly fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.cargarCatalogoRegiones();
  }

  abrirModalRegistro(): void {
    this.modalRegistroAbierto = true;
    this.mensaje = '';
    this.formularioRegistro.reset({ codigoPdv: '', direccionIp: '', comunidadSnmp: 'public' });
  }

  cerrarModalRegistro(): void {
    this.modalRegistroAbierto = false;
    this.guardandoEquipo = false;
  }

  normalizarCodigoPdv(): void {
    const control = this.formularioRegistro.controls.codigoPdv;
    control.setValue(control.value.toUpperCase().replace(/\s/g, ''), { emitEvent: false });
  }

  registrarEquipoTecnico(): void {
    this.normalizarCodigoPdv();
    if (this.formularioRegistro.invalid) {
      this.formularioRegistro.markAllAsTouched();
      return;
    }

    this.guardandoEquipo = true;
    this.error = '';
    this.api.registrarEquipoTecnico(this.formularioRegistro.getRawValue())
      .pipe(finalize(() => this.guardandoEquipo = false))
      .subscribe({
        next: () => {
          this.mensaje = 'Equipo tecnico registrado.';
          this.cerrarModalRegistro();
          if (this.provinciaSeleccionada) {
            this.cargarTarjetas(this.provinciaSeleccionada);
          }
        },
        error: (respuesta) => {
          this.error = respuesta?.error?.message ?? 'No se pudo registrar el equipo tecnico.';
        }
      });
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

  get tarjetasFiltradas(): TarjetaEquipo[] {
    const filtro = this.filtroBusqueda.trim().toLowerCase();
    if (!filtro) {
      return this.tarjetas;
    }

    return this.tarjetas.filter((tarjeta) =>
      tarjeta.nombreEquipo.toLowerCase().includes(filtro)
      || tarjeta.direccionIp.toLowerCase().includes(filtro)
      || tarjeta.codigoSucursal.toLowerCase().includes(filtro)
    );
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
