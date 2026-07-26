import './SubNav.css';
import { useSubNav } from './SubNav.hooks';

function SubNav() {
  const { links } = useSubNav();

  return (
    <div className="subnav" id="subnav">
      <div className="subnav__left">
        <a href="#" className="subnav__link subnav__link--menu" id="subnav-menu">
          <span className="subnav__hamburger">☰</span>
          All
        </a>
        {links.map((link, i) => (
          <a
            href="#"
            className="subnav__link"
            key={i}
            id={`subnav-link-${i}`}
          >
            {link}
          </a>
        ))}
      </div>
    </div>
  );
}

export default SubNav;
