import type { ReactNode } from 'react';

export const Loading = ({ text = 'Loading…' }: { text?: string }) => (
  <div className="state loading" role="status"><span className="spinner" aria-hidden /> {text}</div>
);
export const ErrorBox = ({ message }: { message: string }) => (
  <div className="state error" role="alert">{message}</div>
);
export const Empty = ({ children }: { children: ReactNode }) => <div className="state empty">{children}</div>;
export const Section = ({ title, children }: { title: string; children: ReactNode }) => (
  <section className="card"><h2>{title}</h2>{children}</section>
);
