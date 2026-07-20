import './Footer.css';

function Footer() {
  const footerColumns = [
    {
      title: "Get to Know Us",
      links: ["Careers", "Blog", "About Amazon", "Investor Relations", "Amazon Devices", "Amazon Science"],
    },
    {
      title: "Make Money with Us",
      links: ["Sell products on Amazon", "Sell on Amazon Business", "Sell apps on Amazon", "Become an Affiliate", "Advertise Your Products", "Self-Publish with Us", "Host an Amazon Hub"],
    },
    {
      title: "Amazon Payment Products",
      links: ["Amazon Business Card", "Shop with Points", "Reload Your Balance", "Amazon Currency Converter"],
    },
    {
      title: "Let Us Help You",
      links: ["Amazon and COVID-19", "Your Account", "Your Orders", "Shipping Rates & Policies", "Returns & Replacements", "Manage Your Content", "Help"],
    },
  ];

  return (
    <footer className="footer" id="footer">
      {/* Back to Top */}
      <button
        className="footer__back-to-top"
        onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
        id="footer-back-to-top"
      >
        Back to top
      </button>

      {/* Links Section */}
      <div className="footer__links">
        <div className="footer__links-inner">
          {footerColumns.map((col, idx) => (
            <div className="footer__column" key={idx}>
              <h4 className="footer__column-title">{col.title}</h4>
              <ul className="footer__column-list">
                {col.links.map((link, i) => (
                  <li key={i}>
                    <a href="#" className="footer__column-link">{link}</a>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </div>

      {/* Bottom Section */}
      <div className="footer__bottom">
        <div className="footer__bottom-inner">
          <div className="footer__logo">
            <span className="footer__logo-text">amaazon</span>
            <span className="footer__logo-suffix">.clone</span>
          </div>
          <div className="footer__settings">
            <button className="footer__setting-btn">🇺🇸 English</button>
            <button className="footer__setting-btn">$ USD - U.S. Dollar</button>
            <button className="footer__setting-btn">🇺🇸 United States</button>
          </div>
        </div>

        {/* Sign-in Banner */}
        <div className="footer__signin-banner">
          <p className="footer__signin-text">See personalized recommendations</p>
          <button className="footer__signin-btn" id="footer-signin-btn">Sign in</button>
          <p className="footer__signin-subtext">New customer? <a href="#" className="footer__signin-link">Start here.</a></p>
        </div>

        {/* Copyright */}
        <div className="footer__copyright">
          <div className="footer__copyright-links">
            <a href="#">Conditions of Use</a>
            <a href="#">Privacy Notice</a>
            <a href="#">Consumer Health Data Privacy Disclosure</a>
            <a href="#">Your Ads Privacy Choices</a>
          </div>
          <p>© 1996-2025, Amaazon.clone or its affiliates</p>
        </div>
      </div>
    </footer>
  );
}

export default Footer;
