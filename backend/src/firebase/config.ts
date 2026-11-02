export interface FirebaseWebConfig {
  readonly apiKey: string;
  readonly authDomain: string;
  readonly projectId: string;
  readonly appId: string;
  readonly storageBucket?: string;
  readonly messagingSenderId?: string;
  readonly authEmulatorUrl?: string;
}

export type FirebaseRuntimeConfig =
  | { readonly mode: "disabled" }
  | {
      readonly mode: "emulator";
      readonly projectId: string;
      readonly authEmulatorHost: string;
      readonly firestoreEmulatorHost: string;
    }
  | {
      readonly mode: "adc";
      readonly projectId: string;
    };

