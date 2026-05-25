function statArt(title = "", index = 0) {
  const normalized = title.toLowerCase();
  const type = normalized.includes("vescine") || normalized.includes("skills")
    ? "skills"
    : normalized.includes("vloge") || normalized.includes("roles")
      ? "roles"
      : normalized.includes("mestih") || normalized.includes("regijah") || normalized.includes("cities") || normalized.includes("regions")
        ? "map"
        : normalized.includes("izkusenj") || normalized.includes("experience")
          ? "bars"
          : normalized.includes("tip dela") || normalized.includes("work type")
            ? "pie"
            : "building";

  if (type === "skills") {
    return (
      <div className="stat-art skills-art" aria-hidden="true">
        <svg viewBox="0 0 164 132">
          <path className="line faint" d="M12 128C36 56 84 22 164 14" />
          <path className="line faint" d="M40 128C60 76 100 48 164 42" />
          <path className="line faint" d="M70 128C86 96 118 74 164 70" />
          <path className="line faint" d="M102 128C112 112 132 100 164 100" />
          <circle className="dot glow" cx="152" cy="15" r="3.2" />
          <circle className="dot glow" cx="103" cy="66" r="3.2" />
          <circle className="dot glow" cx="128" cy="94" r="3.2" />
          <circle className="dot glow" cx="86" cy="128" r="3.2" />
        </svg>
      </div>
    );
  }

  if (type === "roles") {
    return (
      <div className="stat-art roles-art" aria-hidden="true">
        <svg viewBox="0 0 132 132">
          <circle className="line faint" cx="66" cy="66" r="60" />
          <circle className="line faint" cx="66" cy="66" r="43" />
          <circle className="line faint" cx="66" cy="66" r="26" />
          <circle className="line faint" cx="66" cy="66" r="9" />
          <path className="line faint" d="M66 6V126" />
          <path className="line faint" d="M6 66H126" />
          <circle className="dot glow" cx="66" cy="6" r="4" />
          <circle className="dot glow" cx="66" cy="38" r="4" />
          <circle className="dot glow" cx="66" cy="66" r="4" />
          <circle className="dot glow" cx="66" cy="96" r="4" />
          <circle className="dot glow" cx="66" cy="126" r="4" />
        </svg>
      </div>
    );
  }

  if (type === "map") {
    const points = [
      [68, 8], [78, 18], [88, 28], [74, 36], [104, 38], [96, 50], [112, 60],
      [124, 74], [86, 72], [100, 84], [116, 96], [130, 108], [78, 102],
      [92, 116], [110, 124], [58, 66], [48, 80], [62, 92], [54, 112],
      [34, 86], [24, 104],
    ];

    return (
      <div className="stat-art map-art" aria-hidden="true">
        <svg viewBox="0 0 142 132">
          <g className="glow" fill="#7c4dff">
            {points.map(([cx, cy]) => <circle key={`${cx}-${cy}`} cx={cx} cy={cy} r="1.7" />)}
          </g>
        </svg>
      </div>
    );
  }

  if (type === "bars") {
    return (
      <div className="stat-art bars-art" aria-hidden="true">
        <svg viewBox="0 0 126 128">
          <defs>
            <linearGradient id={`barGradient-${index}`} x1="0" y1="0" x2="0" y2="1">
              <stop stopColor="#8b5cf6" />
              <stop offset="1" stopColor="#2d1b65" />
            </linearGradient>
          </defs>
          <rect className="bar" style={{ fill: `url(#barGradient-${index})` }} x="3" y="108" width="14" height="20" rx="2" />
          <rect className="bar" style={{ fill: `url(#barGradient-${index})` }} x="28" y="73" width="15" height="55" rx="2" />
          <rect className="bar" style={{ fill: `url(#barGradient-${index})` }} x="54" y="32" width="16" height="96" rx="2" />
          <rect className="bar" style={{ fill: `url(#barGradient-${index})` }} x="82" y="73" width="15" height="55" rx="2" />
          <rect className="bar" style={{ fill: `url(#barGradient-${index})` }} x="108" y="0" width="16" height="128" rx="2" />
        </svg>
      </div>
    );
  }

  if (type === "pie") {
    return (
      <div className="stat-art pie-art" aria-hidden="true">
        <svg viewBox="0 0 130 130">
          <circle className="line faint" cx="65" cy="65" r="58" />
          <path d="M65 65V7A58 58 0 0 1 123 65H65Z" fill="rgba(124,77,255,.16)" stroke="#7650ff" strokeWidth="1.15" />
          <path className="line faint" d="M65 65L24 108" />
          <path className="dot glow" d="M23 109L34 105L27 116Z" />
        </svg>
      </div>
    );
  }

  return (
    <div className="stat-art building-art" aria-hidden="true">
      <svg viewBox="0 0 154 128">
        <rect className="build-fill" x="30" y="18" width="72" height="104" rx="3" />
        <rect className="build-fill" x="108" y="55" width="42" height="67" rx="3" opacity=".78" />
        <rect className="dot glow" x="52" y="42" width="34" height="6" rx="1" />
        <rect className="dot glow" x="52" y="68" width="34" height="6" rx="1" />
        <rect className="dot glow" x="52" y="94" width="34" height="6" rx="1" />
        <rect className="dot glow" x="120" y="78" width="18" height="5" rx="1" />
        <rect className="dot glow" x="120" y="100" width="18" height="5" rx="1" />
        <path className="line faint" d="M0 122H154" />
      </svg>
    </div>
  );
}

export default function MotionStats({ cards = [] }) {
  return (
    <section className="motion-stats">
      {cards.map((card, index) => (
        <article className={`tone-${card.tone}`} key={card.title} style={{ animationDelay: `${index * 110}ms` }}>
          <div className="stat-content">
            <span>{card.title}</span>
            <strong>{card.value}</strong>
            <p>{card.detail}</p>
          </div>
          {statArt(card.title, index)}
        </article>
      ))}
    </section>
  );
}
