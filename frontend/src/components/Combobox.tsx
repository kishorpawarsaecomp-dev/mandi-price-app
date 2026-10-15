interface Props {
  id: string; label: string; value: string; options: string[];
  onChange: (v: string) => void; disabled?: boolean; loading?: boolean; placeholder?: string;
}

/** Type to filter (e.g. "Tur"), then pick the official API name from the list. */
export default function Combobox({ id, label, value, options, onChange, disabled, loading, placeholder }: Props) {
  return (
    <label className="field" htmlFor={id}>
      <span>{label}{loading ? ' …' : ''}</span>
      <input id={id} list={`${id}-list`} value={value} disabled={disabled} placeholder={placeholder}
             onChange={(e) => onChange(e.target.value)} autoComplete="off" />
      <datalist id={`${id}-list`}>{options.map((o) => <option key={o} value={o} />)}</datalist>
    </label>
  );
}
