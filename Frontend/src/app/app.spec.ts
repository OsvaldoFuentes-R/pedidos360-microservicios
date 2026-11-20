import { TestBed } from '@angular/core/testing';
import { MsalService } from '@azure/msal-angular';
import { App } from './app';

describe('App', () => {
  const cuentas: any[] = [];
  const msalFalso = { instance: { getAllAccounts: () => cuentas, setActiveAccount: () => {} } };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [{ provide: MsalService, useValue: msalFalso }],
    }).compileComponents();
  });

  it('crea la app', () => {
    expect(TestBed.createComponent(App).componentInstance).toBeTruthy();
  });

  it('sin sesión muestra el botón de login', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Iniciar sesión con Microsoft');
  });
});
