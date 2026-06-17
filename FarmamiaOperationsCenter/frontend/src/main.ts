import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { AppComponent } from './app/app.component';
import { rutasAdmin } from './app/rutas/rutas-admin';
import { interceptorAuthAdmin } from './app/servicios/interceptor-auth-admin';

bootstrapApplication(AppComponent, {
  providers: [
    provideAnimations(),
    provideRouter(rutasAdmin),
    provideHttpClient(withInterceptors([interceptorAuthAdmin]))
  ]
}).catch((error) => console.error(error));
