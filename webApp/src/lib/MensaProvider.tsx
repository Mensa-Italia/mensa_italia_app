import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { Mensa, type AuthStateKind, type MensaWebUser } from "./mensa";
export type { MensaWebUser, AuthStateKind } from "./mensa";

type Ctx = {
  ready: boolean;
  authState: AuthStateKind;
  user: MensaWebUser | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
};

const MensaCtx = createContext<Ctx | null>(null);

export function MensaProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false);
  const [authState, setAuthState] = useState<AuthStateKind>("Unknown");
  const [user, setUser] = useState<MensaWebUser | null>(null);

  useEffect(() => {
    let cancelAuth: () => void = () => {};
    let cancelUser: () => void = () => {};
    Mensa.initialize().then(() => {
      cancelAuth = Mensa.auth.subscribeAuthState(setAuthState);
      cancelUser = Mensa.auth.subscribeCurrentUser(setUser);
      setReady(true);
    });
    return () => {
      cancelAuth();
      cancelUser();
    };
  }, []);

  return (
    <MensaCtx.Provider
      value={{
        ready,
        authState,
        user,
        login: (e, p) => Mensa.auth.login(e, p).then(() => {}),
        logout: () => Mensa.auth.logout(),
      }}
    >
      {children}
    </MensaCtx.Provider>
  );
}

export function useMensa() {
  const ctx = useContext(MensaCtx);
  if (!ctx) throw new Error("useMensa() outside MensaProvider");
  return ctx;
}
