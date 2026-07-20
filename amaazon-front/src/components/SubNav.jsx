import './SubNav.css';

function SubNav() {
  const links = [
    "Today's Deals",
    "Prime Video",
    "Gift Cards",
    "Sell",
    "Customer Service",
    "Registry",
    "Buy Again",
    "Browsing History",
    "Amazon Basics",
    "Grocery & Gourmet Food",
    "Coupons",
    "Home Improvement",
  ];

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
