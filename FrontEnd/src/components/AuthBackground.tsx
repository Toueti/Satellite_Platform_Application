import Image from 'next/image'

export default function AuthBackground() {
  return (
    <div className="auth-background">
      <Image
        src="/images/bg-pattern-1.png"
        alt="Background pattern 1"
        width={484}
        height={266}
        className="background-image-1"
      />
      <Image
        src="/images/bg-pattern-2.png"
        alt="Background pattern 2"
        width={866}
        height={649}
        className="background-image-2"
      />
      <Image
        src="/images/bg-pattern-3.png"
        alt="Background pattern 3"
        width={239}
        height={179}
        className="background-image-3"
      />
    </div>
  )
}
